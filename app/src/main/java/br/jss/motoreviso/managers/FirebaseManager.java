package br.jss.motoreviso.managers;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import br.jss.motoreviso.models.Equipamento;
import br.jss.motoreviso.models.ImagemVeiculo;
import br.jss.motoreviso.models.Manutencao;
import br.jss.motoreviso.models.Piloto;
import br.jss.motoreviso.models.Trajeto;
import br.jss.motoreviso.models.Veiculo;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_VEICULOS = "veiculos";
    private static final String COLLECTION_MANUTENCOES = "manutencoes";
    private static final String COLLECTION_TRAJETOS = "trajetos";
    private static final String COLLECTION_IMAGENS = "imagens";
    private static final String COLLECTION_PILOTOS = "pilotos";
    private static final String COLLECTION_EQUIPAMENTOS = "equipamentos";
    private static final String STORAGE_VEICULOS = "veiculo_images";

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    private final FirebaseAuth auth;

    private FirebaseManager() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Obtém o UID do usuário autenticado
     * @return UID do usuário autenticado
     * @throws IllegalStateException se usuário não está autenticado
     */
    private String getUsuarioUid() {
        String uid = auth.getCurrentUser() != null ?
            auth.getCurrentUser().getUid() : null;

        if (uid == null) {
            throw new IllegalStateException("Usuário não autenticado. Faça login antes de acessar dados.");
        }
        return uid;
    }

    private static class InstanceHolder {
        static final FirebaseManager INSTANCE = new FirebaseManager();
    }

    public static FirebaseManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // ============ VEÍCULOS (com isolamento por usuário) ============

    /**
     * Adiciona um veículo novo para o usuário autenticado
     */
    public Task<DocumentReference> adicionarVeiculo(Veiculo veiculo) {
        if (veiculo == null) {
            return Tasks.forException(new IllegalArgumentException("Veículo não pode ser nulo"));
        }

        try {
            String uid = getUsuarioUid();
            veiculo.setUserId(uid);

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_VEICULOS)
                    .add(veiculo);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Atualiza um veículo existente (usando o ID do veiculo)
     */
    public Task<Void> atualizarVeiculo(Veiculo veiculo) {
        if (veiculo == null || veiculo.getId() == null || veiculo.getId().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Veiculo ou ID inválido"));
        }

        try {
            String uid = getUsuarioUid();
            veiculo.setUserId(uid);

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_VEICULOS)
                    .document(veiculo.getId())
                    .set(veiculo);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Define um veículo como principal e desmarca todos os outros atomicamente.
     * Usa WriteBatch para garantir consistência: todos os updates vão juntos ou nenhum vai.
     * @param veiculoIdPrincipal ID do veículo que será o novo principal
     * @param todosOsVeiculos lista local com todos os veículos do usuário (precisam ter ID)
     */
    public Task<Void> definirVeiculoPrincipal(String veiculoIdPrincipal, List<br.jss.motoreviso.models.Veiculo> todosOsVeiculos) {
        try {
            String uid = getUsuarioUid();
            WriteBatch batch = db.batch();

            for (br.jss.motoreviso.models.Veiculo v : todosOsVeiculos) {
                if (v.getId() == null || v.getId().isEmpty()) continue;
                DocumentReference ref = db.collection(COLLECTION_USERS)
                        .document(uid)
                        .collection(COLLECTION_VEICULOS)
                        .document(v.getId());
                boolean isPrincipal = v.getId().equals(veiculoIdPrincipal);
                batch.update(ref, "principal", isPrincipal);
            }

            return batch.commit();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Atualiza um veículo existente (usando o ID separado)
     */
    public Task<Void> atualizarVeiculo(String veiculoId, Veiculo veiculo) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }
        if (veiculo == null) {
            return Tasks.forException(new IllegalArgumentException("Veículo não pode ser nulo"));
        }

        try {
            String uid = getUsuarioUid();
            veiculo.setUserId(uid);

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_VEICULOS)
                    .document(veiculoId)
                    .set(veiculo);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Obtém um veículo específico do usuário autenticado
     */
    public Task<DocumentSnapshot> obterVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }

        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_VEICULOS)
                    .document(veiculoId)
                    .get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Obtém todos os veículos ativos do usuário autenticado
     */
    public Task<QuerySnapshot> obterTodosVeiculos() {
        try {
            String uid = getUsuarioUid();

            // Source.SERVER garante que sempre busca do Firestore, ignorando cache local
            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_VEICULOS)
                    .whereEqualTo("ativo", true)
                    .get(com.google.firebase.firestore.Source.SERVER);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Deleta um veículo do usuário autenticado
     */
    public Task<Void> deletarVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }

        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_VEICULOS)
                    .document(veiculoId)
                    .delete();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    // ============ MANUTENÇÕES (com isolamento por usuário) ============

    /**
     * Adiciona uma manutenção nova para o usuário autenticado
     */
    public Task<DocumentReference> adicionarManutencao(Manutencao manutencao) {
        if (manutencao == null) {
            return Tasks.forException(new IllegalArgumentException("Manutenção não pode ser nula"));
        }

        try {
            String uid = getUsuarioUid();
            manutencao.setUserId(uid);

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_MANUTENCOES)
                    .add(manutencao);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Obtém todas as manutenções de um veículo específico do usuário autenticado
     */
    public Task<QuerySnapshot> obterManutencoesVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }

        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_MANUTENCOES)
                    .whereEqualTo("veiculoId", veiculoId)
                    .get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Obtém todas as manutenções do usuário autenticado
     * Ordenação feita no cliente para evitar necessidade de índice composto
     */
    public Task<QuerySnapshot> obterTodasManutencoes() {
        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_MANUTENCOES)
                    .get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /** @deprecated Use {@link #obterTodasManutencoes()} */
    @Deprecated
    public Task<QuerySnapshot> obterTodasManutemcoes() {
        return obterTodasManutencoes();
    }

    /**
     * Atualiza uma manutenção existente
     */
    public Task<Void> atualizarManutencao(String manutencaoId, Manutencao manutencao) {
        if (manutencaoId == null || manutencaoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID da manutenção inválido"));
        }
        if (manutencao == null) {
            return Tasks.forException(new IllegalArgumentException("Manutenção não pode ser nula"));
        }

        try {
            String uid = getUsuarioUid();
            manutencao.setUserId(uid);

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_MANUTENCOES)
                    .document(manutencaoId)
                    .set(manutencao);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Obtém uma manutenção específica do usuário autenticado
     */
    public Task<DocumentSnapshot> obterManutencao(String manutencaoId) {
        if (manutencaoId == null || manutencaoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID da manutenção inválido"));
        }

        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_MANUTENCOES)
                    .document(manutencaoId)
                    .get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Deleta uma manutenção do usuário autenticado
     */
    public Task<Void> deletarManutencao(String manutencaoId) {
        if (manutencaoId == null || manutencaoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID da manutenção inválido"));
        }

        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_MANUTENCOES)
                    .document(manutencaoId)
                    .delete();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    // ============ TRAJETOS (com isolamento por usuário) ============

    /**
     * Adiciona um trajeto novo para o usuário autenticado
     */
    public Task<DocumentReference> adicionarTrajeto(Trajeto trajeto) {
        if (trajeto == null) {
            return Tasks.forException(new IllegalArgumentException("Trajeto não pode ser nulo"));
        }

        try {
            String uid = getUsuarioUid();
            trajeto.setUserId(uid);

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_TRAJETOS)
                    .add(trajeto);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Busca todos os trajetos do usuário autenticado.
     * Ordenação feita no cliente para evitar necessidade de índice.
     */
    public Task<QuerySnapshot> obterTrajetosUsuario(String userId) {
        // Nota: Este método é mantido para compatibilidade, mas usa o UID autenticado
        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_TRAJETOS)
                    .get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Busca trajetos de um veículo específico do usuário autenticado.
     * Ordenação feita no cliente para evitar necessidade de índice composto.
     */
    public Task<QuerySnapshot> obterTrajetosVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }

        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_TRAJETOS)
                    .whereEqualTo("veiculoId", veiculoId)
                    .get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Atualiza um trajeto existente
     */
    public Task<Void> atualizarTrajeto(String trajetoId, Trajeto trajeto) {
        if (trajetoId == null || trajetoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do trajeto inválido"));
        }
        if (trajeto == null) {
            return Tasks.forException(new IllegalArgumentException("Trajeto não pode ser nulo"));
        }

        try {
            String uid = getUsuarioUid();
            trajeto.setUserId(uid);

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_TRAJETOS)
                    .document(trajetoId)
                    .set(trajeto);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Obtém um trajeto específico do usuário autenticado
     */
    public Task<DocumentSnapshot> obterTrajeto(String trajetoId) {
        if (trajetoId == null || trajetoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do trajeto inválido"));
        }

        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_TRAJETOS)
                    .document(trajetoId)
                    .get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Deleta um trajeto do usuário autenticado
     */
    public Task<Void> deletarTrajeto(String trajetoId) {
        if (trajetoId == null || trajetoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do trajeto inválido"));
        }

        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_TRAJETOS)
                    .document(trajetoId)
                    .delete();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    // ============ IMAGENS (com isolamento por usuário) ============

    /**
     * Adiciona uma imagem de veículo para o usuário autenticado
     */
    public Task<DocumentReference> adicionarImagemVeiculo(ImagemVeiculo imagem) {
        if (imagem == null) {
            return Tasks.forException(new IllegalArgumentException("Imagem não pode ser nula"));
        }

        try {
            String uid = getUsuarioUid();
            imagem.setUserId(uid);

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_IMAGENS)
                    .add(imagem);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Obtém todas as imagens de um veículo específico do usuário autenticado
     * Ordenação: mais recentes primeiro
     */
    public Task<QuerySnapshot> obterImagensVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }

        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_IMAGENS)
                    .whereEqualTo("veiculoId", veiculoId)
                    .get();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Deleta uma imagem do usuário autenticado
     */
    public Task<Void> deletarImagemVeiculo(String imagemId) {
        if (imagemId == null || imagemId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID da imagem inválido"));
        }

        try {
            String uid = getUsuarioUid();

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_IMAGENS)
                    .document(imagemId)
                    .delete();
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    /**
     * Atualiza uma imagem existente
     */
    public Task<Void> atualizarImagemVeiculo(String imagemId, ImagemVeiculo imagem) {
        if (imagemId == null || imagemId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID da imagem inválido"));
        }
        if (imagem == null) {
            return Tasks.forException(new IllegalArgumentException("Imagem não pode ser nula"));
        }

        try {
            String uid = getUsuarioUid();
            imagem.setUserId(uid);

            return db.collection(COLLECTION_USERS)
                    .document(uid)
                    .collection(COLLECTION_IMAGENS)
                    .document(imagemId)
                    .set(imagem);
        } catch (IllegalStateException e) {
            return Tasks.forException(e);
        }
    }

    // ============ PILOTOS (com isolamento por usuário) ============

    public Task<DocumentReference> adicionarPiloto(Piloto piloto) {
        if (piloto == null) return Tasks.forException(new IllegalArgumentException("Piloto não pode ser nulo"));
        try {
            String uid = getUsuarioUid();
            piloto.setUserId(uid);
            return db.collection(COLLECTION_USERS).document(uid)
                    .collection(COLLECTION_PILOTOS).add(piloto);
        } catch (IllegalStateException e) { return Tasks.forException(e); }
    }

    public Task<DocumentSnapshot> obterPiloto(String pilotoId) {
        if (pilotoId == null || pilotoId.isEmpty())
            return Tasks.forException(new IllegalArgumentException("ID do piloto inválido"));
        try {
            String uid = getUsuarioUid();
            return db.collection(COLLECTION_USERS).document(uid)
                    .collection(COLLECTION_PILOTOS).document(pilotoId).get();
        } catch (IllegalStateException e) { return Tasks.forException(e); }
    }

    public Task<QuerySnapshot> obterTodosPilotos() {
        try {
            String uid = getUsuarioUid();
            return db.collection(COLLECTION_USERS).document(uid)
                    .collection(COLLECTION_PILOTOS).get();
        } catch (IllegalStateException e) { return Tasks.forException(e); }
    }

    public Task<Void> atualizarPiloto(Piloto piloto) {
        if (piloto == null || piloto.getId() == null)
            return Tasks.forException(new IllegalArgumentException("Piloto ou ID inválido"));
        try {
            String uid = getUsuarioUid();
            piloto.setUserId(uid);
            return db.collection(COLLECTION_USERS).document(uid)
                    .collection(COLLECTION_PILOTOS).document(piloto.getId()).set(piloto);
        } catch (IllegalStateException e) { return Tasks.forException(e); }
    }

    public Task<Void> deletarPiloto(String pilotoId) {
        if (pilotoId == null || pilotoId.isEmpty())
            return Tasks.forException(new IllegalArgumentException("ID do piloto inválido"));
        try {
            String uid = getUsuarioUid();
            return db.collection(COLLECTION_USERS).document(uid)
                    .collection(COLLECTION_PILOTOS).document(pilotoId).delete();
        } catch (IllegalStateException e) { return Tasks.forException(e); }
    }

    // ============ EQUIPAMENTOS (com isolamento por usuário) ============

    public Task<DocumentReference> adicionarEquipamento(Equipamento equipamento) {
        if (equipamento == null) return Tasks.forException(new IllegalArgumentException("Equipamento não pode ser nulo"));
        try {
            String uid = getUsuarioUid();
            equipamento.setUserId(uid);
            return db.collection(COLLECTION_USERS).document(uid)
                    .collection(COLLECTION_EQUIPAMENTOS).add(equipamento);
        } catch (IllegalStateException e) { return Tasks.forException(e); }
    }

    public Task<QuerySnapshot> obterEquipamentosPiloto(String pilotoId) {
        if (pilotoId == null || pilotoId.isEmpty())
            return Tasks.forException(new IllegalArgumentException("ID do piloto inválido"));
        try {
            String uid = getUsuarioUid();
            return db.collection(COLLECTION_USERS).document(uid)
                    .collection(COLLECTION_EQUIPAMENTOS)
                    .whereEqualTo("pilotoId", pilotoId).get();
        } catch (IllegalStateException e) { return Tasks.forException(e); }
    }

    public Task<Void> deletarEquipamento(String equipamentoId) {
        if (equipamentoId == null || equipamentoId.isEmpty())
            return Tasks.forException(new IllegalArgumentException("ID do equipamento inválido"));
        try {
            String uid = getUsuarioUid();
            return db.collection(COLLECTION_USERS).document(uid)
                    .collection(COLLECTION_EQUIPAMENTOS).document(equipamentoId).delete();
        } catch (IllegalStateException e) { return Tasks.forException(e); }
    }

    // ============ STORAGE ============

    // Nota: Salvando imagens localmente no dispositivo, não em Firebase Storage
    // O caminho é armazenado no Firestore para recuperação posterior

    public void uploadImagemVeiculo(Uri uri, String nomeArquivo,
                                    OnUploadCompleteListener callback) {
        // Este método não é mais usado - use salvarImagemLocalmente() em vez disso
        if (callback != null) {
            callback.onUploadFailed(new IllegalArgumentException("Use salvarImagemLocalmente() em vez disso"));
        }
    }

    // Novo método: Salvar imagem localmente no dispositivo
    public void salvarImagemLocalmente(android.content.Context context, Uri imageUri,
                                       String nomeArquivo, OnUploadCompleteListener callback) {
        try {
            Log.d(TAG, "Iniciando salvamento local de imagem: " + nomeArquivo);

            // Obter diretório de imagens do app
            java.io.File imagensDir = new java.io.File(context.getExternalFilesDir(null), "imagens_veiculos");

            // Criar diretório se não existir
            if (!imagensDir.exists()) {
                imagensDir.mkdirs();
                Log.d(TAG, "Diretório criado: " + imagensDir.getAbsolutePath());
            }

            // Arquivo de destino
            java.io.File arquivoDestino = new java.io.File(imagensDir, nomeArquivo);

            // Copiar arquivo
            try (java.io.InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                 java.io.FileOutputStream outputStream = new java.io.FileOutputStream(arquivoDestino)) {

                if (inputStream == null) {
                    throw new IllegalArgumentException("Não foi possível abrir o arquivo de imagem");
                }

                byte[] buffer = new byte[1024];
                int bytesLidos;
                while ((bytesLidos = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesLidos);
                }

                outputStream.flush();
            }

            String caminhoCompleto = arquivoDestino.getAbsolutePath();
            Log.d(TAG, "Imagem salva com sucesso em: " + caminhoCompleto);

            if (callback != null) {
                callback.onUploadComplete(caminhoCompleto);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar imagem localmente", e);
            if (callback != null) {
                callback.onUploadFailed(e);
            }
        }
    }

    public Task<Void> deletarImagemStorage(String urlImagem) {
        if (urlImagem == null || urlImagem.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("URL de imagem inválida"));
        }
        try {
            StorageReference reference = storage.getReferenceFromUrl(urlImagem);
            return reference.delete();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "URL de imagem inválida: " + urlImagem, e);
            return Tasks.forException(e);
        }
    }

    // ============ CALLBACKS ============

    public interface OnUploadCompleteListener {
        void onUploadComplete(String downloadUrl);
        void onUploadFailed(Exception exception);
    }

    public interface VeiculoCallback {
        void onSuccess(Veiculo veiculo);
        void onError(String error);
    }

    public interface TrajetosCallback {
        void onSuccess(List<DocumentSnapshot> trajetos);
        void onError(String error);
    }

    // ============ CONVENIENCE METHODS WITH CALLBACKS ============

    /**
     * Carrega o primeiro veículo marcado como ativo do usuário autenticado
     */
    public void carregarVeiculoPrincipal(VeiculoCallback callback) {
        obterTodosVeiculos()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.getDocuments().isEmpty()) {
                        Veiculo veiculo = querySnapshot.getDocuments().get(0).toObject(Veiculo.class);
                        if (veiculo != null) {
                            veiculo.setId(querySnapshot.getDocuments().get(0).getId());
                        }
                        if (callback != null) {
                            callback.onSuccess(veiculo);
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao carregar veículo principal", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    /**
     * Carrega o veículo marcado como principal (principal = true)
     * Se não houver marcado, retorna o primeiro ativo
     */
    public void carregarVeiculoPrincipalMarcado(VeiculoCallback callback) {
        obterTodosVeiculos()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Veiculo veiculo = doc.toObject(Veiculo.class);
                            if (veiculo != null && veiculo.isPrincipal()) {
                                if (callback != null) {
                                    veiculo.setId(doc.getId());
                                    callback.onSuccess(veiculo);
                                }
                                return;
                            }
                        }
                    }
                    // Se nenhum marcado como principal, retorna o primeiro
                    if (querySnapshot != null && !querySnapshot.getDocuments().isEmpty()) {
                        Veiculo veiculo = querySnapshot.getDocuments().get(0).toObject(Veiculo.class);
                        if (veiculo != null) {
                            veiculo.setId(querySnapshot.getDocuments().get(0).getId());
                        }
                        if (callback != null) {
                            callback.onSuccess(veiculo);
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao carregar veículo principal marcado", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    /**
     * Carrega todos os trajetos do usuário autenticado
     */
    public void carregarTrajetos(TrajetosCallback callback) {
        obterTrajetosUsuario(null)
                .addOnSuccessListener(querySnapshot -> {
                    if (callback != null) {
                        callback.onSuccess(querySnapshot != null ? querySnapshot.getDocuments() : new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao carregar trajetos", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }
}
