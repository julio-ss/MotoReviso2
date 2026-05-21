package br.jss.motoreviso.managers;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
    private static final String STORAGE_VEICULOS = "veiculos";

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
        return db.collection(COLLECTION_MANUTENCOES)
                .whereEqualTo("veiculoId", veiculoId)
                .orderBy("dataRevisao", Query.Direction.DESCENDING)
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

    public Task<QuerySnapshot> obterTodosTrajetos() {
        return db.collection(COLLECTION_TRAJETOS)
                .orderBy("dataInicio", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> obterTrajetosVeiculo(String veiculoId) {
        if (veiculoId == null || veiculoId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("ID do veículo inválido"));
        }
        return db.collection(COLLECTION_TRAJETOS)
                .whereEqualTo("veiculoId", veiculoId)
                .orderBy("dataInicio", Query.Direction.DESCENDING)
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

        StorageReference reference = storage.getReference()
                .child(STORAGE_VEICULOS)
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

    public interface OnUploadCompleteListener {
        void onUploadComplete(String downloadUrl);
        void onUploadFailed(Exception exception);
    }
}
