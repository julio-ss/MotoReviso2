package br.jss.motoreviso.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import br.jss.motoreviso.models.UserProfile;

/**
 * Perfil do usuário em Firestore: documento {@code users/{uid}}
 * (mesma raiz usada pelo FirebaseManager para as subcoleções).
 */
public class UserRepository {

    private static final String COLLECTION_USERS = "users";

    public interface UserCallback {
        void onSuccess(UserProfile profile);
        void onError(String error);
    }

    private final FirebaseFirestore db;

    public UserRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public UserRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public void carregarPerfil(String uid, UserCallback callback) {
        if (uid == null || uid.isEmpty()) {
            callback.onError("Usuário inválido");
            return;
        }
        db.collection(COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    UserProfile profile = toProfile(doc);
                    if (profile != null) callback.onSuccess(profile);
                    else callback.onError("Perfil não encontrado");
                })
                .addOnFailureListener(e -> callback.onError(
                        e.getMessage() != null ? e.getMessage() : "Erro ao carregar perfil"));
    }

    /** Cria ou atualiza o perfil (merge, preserva outros campos do documento). */
    public Task<Void> salvarPerfil(UserProfile profile) {
        return db.collection(COLLECTION_USERS).document(profile.getUid())
                .set(profile, SetOptions.merge());
    }

    private static UserProfile toProfile(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        UserProfile p = doc.toObject(UserProfile.class);
        if (p != null && p.getUid() == null) p.setUid(doc.getId());
        return p;
    }
}
