package br.jss.motoreviso.managers;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import br.jss.motoreviso.models.ImagemVeiculo;
import br.jss.motoreviso.models.Manutencao;
import br.jss.motoreviso.models.Trajeto;
import br.jss.motoreviso.models.Veiculo;

import java.util.List;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private static final String COLLECTION_VEICULOS = "veiculos";
    private static final String COLLECTION_MANUTENCOES = "manutencoes";
    private static final String COLLECTION_TRAJETOS = "trajetos";
    private static final String COLLECTION_IMAGENS = "imagens";

    private static final String STORAGE_VEICULOS = "veiculos";

    private FirebaseManager() {
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // ============ VEÍCULOS ============

    public Task<DocumentReference> adicionarVeiculo(Veiculo veiculo) {
        return db.collection(COLLECTION_VEICULOS).add(veiculo);
    }

    public Task<Void> atualizarVeiculo(String veiculoId, Veiculo veiculo) {
        return db.collection(COLLECTION_VEICULOS).document(veiculoId).set(veiculo);
    }

    public Task<DocumentSnapshot> obterVeiculo(String veiculoId) {
        return db.collection(COLLECTION_VEICULOS).document(veiculoId).get();
    }

    public Task<QuerySnapshot> obterTodosVeiculos() {
        return db.collection(COLLECTION_VEICULOS)
                .whereEqualTo("ativo", true)
                .get();
    }

    public Task<Void> deletarVeiculo(String veiculoId) {
        return db.collection(COLLECTION_VEICULOS).document(veiculoId).delete();
    }

    // ============ MANUTENÇÕES ============

    public Task<DocumentReference> adicionarManutencao(Manutencao manutencao) {
        return db.collection(COLLECTION_MANUTENCOES).add(manutencao);
    }

    public Task<QuerySnapshot> obterManutencoesVeiculo(String veiculoId) {
        return db.collection(COLLECTION_MANUTENCOES)
                .whereEqualTo("veiculoId", veiculoId)
                .orderBy("dataRevisao", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> obterTodasManutemcoes() {
        return db.collection(COLLECTION_MANUTENCOES)
                .orderBy("dataRevisao", Query.Direction.DESCENDING)
                .get();
    }

    public Task<Void> atualizarManutencao(String manutencaoId, Manutencao manutencao) {
        return db.collection(COLLECTION_MANUTENCOES).document(manutencaoId).set(manutencao);
    }

    public Task<DocumentSnapshot> obterManutencao(String manutencaoId) {
        return db.collection(COLLECTION_MANUTENCOES).document(manutencaoId).get();
    }

    public Task<Void> deletarManutencao(String manutencaoId) {
        return db.collection(COLLECTION_MANUTENCOES).document(manutencaoId).delete();
    }

    // ============ TRAJETOS ============

    public Task<DocumentReference> adicionarTrajeto(Trajeto trajeto) {
        return db.collection(COLLECTION_TRAJETOS).add(trajeto);
    }

    public Task<QuerySnapshot> obterTrajetosVeiculo(String veiculoId) {
        return db.collection(COLLECTION_TRAJETOS)
                .whereEqualTo("veiculoId", veiculoId)
                .orderBy("dataInicio", Query.Direction.DESCENDING)
                .get();
    }

    public Task<Void> atualizarTrajeto(String tratejoId, Trajeto trajeto) {
        return db.collection(COLLECTION_TRAJETOS).document(tratejoId).set(trajeto);
    }

    public Task<DocumentSnapshot> obterTrajeto(String tratejoId) {
        return db.collection(COLLECTION_TRAJETOS).document(tratejoId).get();
    }

    public Task<Void> deletarTrajeto(String tratejoId) {
        return db.collection(COLLECTION_TRAJETOS).document(tratejoId).delete();
    }

    // ============ IMAGENS ============

    public Task<DocumentReference> adicionarImagemVeiculo(ImagemVeiculo imagem) {
        return db.collection(COLLECTION_IMAGENS).add(imagem);
    }

    public Task<QuerySnapshot> obterImagensVeiculo(String veiculoId) {
        return db.collection(COLLECTION_IMAGENS)
                .whereEqualTo("veiculoId", veiculoId)
                .orderBy("dataCadastro", Query.Direction.DESCENDING)
                .get();
    }

    public Task<Void> deletarImagemVeiculo(String imagemId) {
        return db.collection(COLLECTION_IMAGENS).document(imagemId).delete();
    }

    public Task<Void> atualizarImagemVeiculo(String imagemId, ImagemVeiculo imagem) {
        return db.collection(COLLECTION_IMAGENS).document(imagemId).set(imagem);
    }

    // ============ STORAGE (Upload de Imagens) ============

    public void uploadImagemVeiculo(Uri uri, String nomeArquivo,
                                    OnUploadCompleteListener callback) {
        StorageReference reference = storage.getReference()
                .child(STORAGE_VEICULOS)
                .child(nomeArquivo);

        reference.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    reference.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        callback.onUploadComplete(downloadUri.toString());
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Erro ao obter URL de download", e);
                        callback.onUploadFailed(e);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro no upload da imagem", e);
                    callback.onUploadFailed(e);
                });
    }

    public Task<Void> deletarImagemStorage(String urlImagem) {
        try {
            StorageReference reference = storage.getReferenceFromUrl(urlImagem);
            return reference.delete();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "URL de imagem inválida", e);
            return null;
        }
    }

    public interface OnUploadCompleteListener {
        void onUploadComplete(String downloadUrl);
        void onUploadFailed(Exception exception);
    }
}