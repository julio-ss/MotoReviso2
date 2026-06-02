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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import br.jss.motoreviso.models.ImagemVeiculo;
import br.jss.motoreviso.models.Manutencao;
import br.jss.motoreviso.models.Trajeto;
import br.jss.motoreviso.models.Veiculo;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";

    private static final String COLLECTION_VEICULOS = "veiculos";
    private static final String COLLECTION_MANUTENCOES = "manutencoes";
    private static final String COLLECTION_TRAJETOS = "trajetos";
    private static final String COLLECTION_IMAGENS = "imagens";
    private static final String STORAGE_VEICULOS = "veiculo_images";

    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    private FirebaseManager() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    private static class InstanceHolder {
        static final FirebaseManager INSTANCE = new FirebaseManager();
    }

    public static FirebaseManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    // ============ VEÍCULOS ============

    public Task<DocumentReference> adicionarVeiculo(Veiculo veiculo) {
        if (veiculo == null) {
            return Tasks.forException(new IllegalArgumentException("Veículo não pode ser nulo"));
        }
        return db.collection(COLLECTION_VEICULOS).add(veiculo);
    }

    public Task<Void> atualizarVeiculo(String veiculoId, Veiculo veiculo) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }
        return db.collection(COLLECTION_VEICULOS).document(veiculoId).set(veiculo);
    }

    public Task<DocumentSnapshot> obterVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }
        return db.collection(COLLECTION_VEICULOS).document(veiculoId).get();
    }

    public Task<QuerySnapshot> obterTodosVeiculos() {
        return db.collection(COLLECTION_VEICULOS)
                .whereEqualTo("ativo", true)
                .get();
    }

    public Task<Void> deletarVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }
        return db.collection(COLLECTION_VEICULOS).document(veiculoId).delete();
    }

    // ============ MANUTENÇÕES ============

    public Task<DocumentReference> adicionarManutencao(Manutencao manutencao) {
        if (manutencao == null) {
            return Tasks.forException(new IllegalArgumentException("Manutenção não pode ser nula"));
        }
        return db.collection(COLLECTION_MANUTENCOES).add(manutencao);
    }

    public Task<QuerySnapshot> obterManutencoesVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }
        // Sem orderBy para evitar necessidade de índice composto — ordena no cliente
        return db.collection(COLLECTION_MANUTENCOES)
                .whereEqualTo("veiculoId", veiculoId)
                .get();
    }

    public Task<QuerySnapshot> obterTodasManutencoes() {
        return db.collection(COLLECTION_MANUTENCOES)
                .orderBy("dataRevisao", Query.Direction.DESCENDING)
                .get();
    }

    /** @deprecated Use {@link #obterTodasManutencoes()} */
    @Deprecated
    public Task<QuerySnapshot> obterTodasManutemcoes() {
        return obterTodasManutencoes();
    }

    public Task<Void> atualizarManutencao(String manutencaoId, Manutencao manutencao) {
        if (manutencaoId == null || manutencaoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID da manutenção inválido"));
        }
        return db.collection(COLLECTION_MANUTENCOES).document(manutencaoId).set(manutencao);
    }

    public Task<DocumentSnapshot> obterManutencao(String manutencaoId) {
        if (manutencaoId == null || manutencaoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID da manutenção inválido"));
        }
        return db.collection(COLLECTION_MANUTENCOES).document(manutencaoId).get();
    }

    public Task<Void> deletarManutencao(String manutencaoId) {
        if (manutencaoId == null || manutencaoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID da manutenção inválido"));
        }
        return db.collection(COLLECTION_MANUTENCOES).document(manutencaoId).delete();
    }

    // ============ TRAJETOS ============

    public Task<DocumentReference> adicionarTrajeto(Trajeto trajeto) {
        if (trajeto == null) {
            return Tasks.forException(new IllegalArgumentException("Trajeto não pode ser nulo"));
        }
        return db.collection(COLLECTION_TRAJETOS).add(trajeto);
    }

    /**
     * Busca todos os trajetos do usuário, sem orderBy para não exigir índice.
     * Ordenação feita no cliente.
     */
    public Task<QuerySnapshot> obterTrajetosUsuario(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("userId inválido"));
        }
        return db.collection(COLLECTION_TRAJETOS)
                .whereEqualTo("userId", userId)
                .get();
    }

    /**
     * Busca trajetos de um veículo específico.
     * Sem orderBy — evita necessidade de índice composto no Firestore.
     * Ordenação feita no cliente.
     */
    public Task<QuerySnapshot> obterTrajetosVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }
        return db.collection(COLLECTION_TRAJETOS)
                .whereEqualTo("veiculoId", veiculoId)
                .get();
    }

    public Task<Void> atualizarTrajeto(String trajetoId, Trajeto trajeto) {
        if (trajetoId == null || trajetoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do trajeto inválido"));
        }
        return db.collection(COLLECTION_TRAJETOS).document(trajetoId).set(trajeto);
    }

    public Task<DocumentSnapshot> obterTrajeto(String trajetoId) {
        if (trajetoId == null || trajetoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do trajeto inválido"));
        }
        return db.collection(COLLECTION_TRAJETOS).document(trajetoId).get();
    }

    public Task<Void> deletarTrajeto(String trajetoId) {
        if (trajetoId == null || trajetoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do trajeto inválido"));
        }
        return db.collection(COLLECTION_TRAJETOS).document(trajetoId).delete();
    }

    // ============ IMAGENS ============

    public Task<DocumentReference> adicionarImagemVeiculo(ImagemVeiculo imagem) {
        if (imagem == null) {
            return Tasks.forException(new IllegalArgumentException("Imagem não pode ser nula"));
        }
        return db.collection(COLLECTION_IMAGENS).add(imagem);
    }

    public Task<QuerySnapshot> obterImagensVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }
        return db.collection(COLLECTION_IMAGENS)
                .whereEqualTo("veiculoId", veiculoId)
                .orderBy("dataCadastro", Query.Direction.DESCENDING)
                .get();
    }

    public Task<Void> deletarImagemVeiculo(String imagemId) {
        if (imagemId == null || imagemId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID da imagem inválido"));
        }
        return db.collection(COLLECTION_IMAGENS).document(imagemId).delete();
    }

    public Task<Void> atualizarImagemVeiculo(String imagemId, ImagemVeiculo imagem) {
        if (imagemId == null || imagemId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID da imagem inválido"));
        }
        return db.collection(COLLECTION_IMAGENS).document(imagemId).set(imagem);
    }

    // ============ STORAGE ============

    public void uploadImagemVeiculo(Uri uri, String nomeArquivo,
                                    OnUploadCompleteListener callback) {
        if (uri == null || nomeArquivo == null || nomeArquivo.isEmpty()) {
            if (callback != null) {
                callback.onUploadFailed(new IllegalArgumentException("URI ou nome de arquivo inválido"));
            }
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "anonymous";

        StorageReference reference = storage.getReference()
                .child(STORAGE_VEICULOS)
                .child(userId)
                .child(nomeArquivo);

        reference.putFile(uri)
                .addOnSuccessListener(taskSnapshot ->
                        reference.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
                                    if (callback != null) {
                                        callback.onUploadComplete(downloadUri.toString());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Erro ao obter URL de download", e);
                                    if (callback != null) callback.onUploadFailed(e);
                                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro no upload da imagem", e);
                    if (callback != null) callback.onUploadFailed(e);
                });
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
     * Carrega o primeiro veículo marcado como principal/ativo do usuário
     */
    public void carregarVeiculoPrincipal(VeiculoCallback callback) {
        obterTodosVeiculos()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot != null && !querySnapshot.getDocuments().isEmpty()) {
                        Veiculo veiculo = querySnapshot.getDocuments().get(0).toObject(Veiculo.class);
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
     * Carrega todos os trajetos do usuário
     */
    public void carregarTrajetos(TrajetosCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            if (callback != null) {
                callback.onError("Usuário não autenticado");
            }
            return;
        }

        obterTrajetosUsuario(userId)
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
