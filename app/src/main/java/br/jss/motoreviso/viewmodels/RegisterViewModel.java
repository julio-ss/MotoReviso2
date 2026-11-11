package br.jss.motoreviso.viewmodels;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.FirebaseNetworkException;

import br.jss.motoreviso.models.UserProfile;
import br.jss.motoreviso.repositories.UserRepository;

/**
 * Cadastro de conta: valida o formulário, cria o usuário no Firebase Auth
 * e grava o perfil em {@code users/{uid}}.
 */
public class RegisterViewModel extends ViewModel {

    private static final int SENHA_MIN = 6; // mínimo exigido pelo Firebase Auth

    private final MutableLiveData<String> _nomeError = new MutableLiveData<>();
    private final MutableLiveData<String> _apelidoError = new MutableLiveData<>();
    private final MutableLiveData<String> _emailError = new MutableLiveData<>();
    private final MutableLiveData<String> _telefoneError = new MutableLiveData<>();
    private final MutableLiveData<String> _cidadeError = new MutableLiveData<>();
    private final MutableLiveData<String> _senhaError = new MutableLiveData<>();
    private final MutableLiveData<String> _confirmarSenhaError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> _successMessage = new MutableLiveData<>();

    public final LiveData<String> nomeError = _nomeError;
    public final LiveData<String> apelidoError = _apelidoError;
    public final LiveData<String> emailError = _emailError;
    public final LiveData<String> telefoneError = _telefoneError;
    public final LiveData<String> cidadeError = _cidadeError;
    public final LiveData<String> senhaError = _senhaError;
    public final LiveData<String> confirmarSenhaError = _confirmarSenhaError;
    public final LiveData<Boolean> isLoading = _isLoading;
    public final LiveData<String> errorMessage = _errorMessage;
    public final LiveData<String> successMessage = _successMessage;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final UserRepository userRepository = new UserRepository();

    // ─────────────────────────────────────────────────────────
    //  Validação
    // ─────────────────────────────────────────────────────────

    /** Valida todos os campos, publica os erros individuais e retorna se está tudo certo. */
    public boolean validarFormulario(String nome, String apelido, String email, String telefone,
                                     String cidade, String senha, String confirmarSenha) {
        boolean ok = true;

        String n = trim(nome);
        if (n.isEmpty()) { _nomeError.setValue("Informe seu nome"); ok = false; }
        else if (n.length() < 3) { _nomeError.setValue("Nome muito curto"); ok = false; }
        else _nomeError.setValue(null);

        String a = trim(apelido);
        if (a.length() > 30) { _apelidoError.setValue("Apelido deve ter até 30 caracteres"); ok = false; }
        else _apelidoError.setValue(null); // opcional

        String e = trim(email);
        if (e.isEmpty()) { _emailError.setValue("Informe seu e-mail"); ok = false; }
        else if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) { _emailError.setValue("E-mail inválido"); ok = false; }
        else _emailError.setValue(null);

        String digitos = trim(telefone).replaceAll("[^0-9]", "");
        if (!digitos.isEmpty() && (digitos.length() < 10 || digitos.length() > 11)) {
            _telefoneError.setValue("Telefone deve ter DDD + número");
            ok = false;
        } else {
            _telefoneError.setValue(null); // opcional
        }

        _cidadeError.setValue(null); // opcional

        String s = senha == null ? "" : senha;
        if (s.isEmpty()) { _senhaError.setValue("Informe uma senha"); ok = false; }
        else if (s.length() < SENHA_MIN) { _senhaError.setValue("A senha deve ter pelo menos " + SENHA_MIN + " caracteres"); ok = false; }
        else _senhaError.setValue(null);

        if (confirmarSenha == null || confirmarSenha.isEmpty()) {
            _confirmarSenhaError.setValue("Confirme a senha");
            ok = false;
        } else if (!confirmarSenha.equals(s)) {
            _confirmarSenhaError.setValue("As senhas não coincidem");
            ok = false;
        } else {
            _confirmarSenhaError.setValue(null);
        }

        if (!ok) _errorMessage.setValue(null);
        return ok;
    }

    /** Máscara brasileira: (11) 91234-5678 ou (11) 1234-5678. */
    public String formatarTelefone(String numeros) {
        String d = numeros == null ? "" : numeros.replaceAll("[^0-9]", "");
        if (d.length() > 11) d = d.substring(0, 11);
        int len = d.length();
        if (len == 0) return "";
        if (len <= 2) return "(" + d;
        String ddd = d.substring(0, 2);
        String resto = d.substring(2);
        if (resto.length() <= 4) return "(" + ddd + ") " + resto;
        int corte = len == 11 ? 5 : 4;
        if (resto.length() <= corte) return "(" + ddd + ") " + resto;
        return "(" + ddd + ") " + resto.substring(0, corte) + "-" + resto.substring(corte);
    }

    // ─────────────────────────────────────────────────────────
    //  Cadastro
    // ─────────────────────────────────────────────────────────

    public void registrar(String nome, String apelido, String email, String telefone,
                          String cidade, String senha) {
        if (Boolean.TRUE.equals(_isLoading.getValue())) return;
        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        final String n = trim(nome);
        final String a = trim(apelido);
        final String e = trim(email);
        final String t = trim(telefone);
        final String c = trim(cidade);

        auth.createUserWithEmailAndPassword(e, senha)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        falha("Não foi possível criar a conta");
                        return;
                    }
                    user.updateProfile(new UserProfileChangeRequest.Builder().setDisplayName(n).build());

                    UserProfile profile = new UserProfile(user.getUid(), n,
                            a.isEmpty() ? null : a, e, t.isEmpty() ? null : t, c.isEmpty() ? null : c);
                    userRepository.salvarPerfil(profile)
                            .addOnCompleteListener(task -> {
                                // A conta já existe no Auth; mesmo se o perfil falhar,
                                // o usuário entra e o perfil pode ser editado depois.
                                _isLoading.setValue(false);
                                _successMessage.setValue("Conta criada com sucesso");
                            });
                })
                .addOnFailureListener(ex -> falha(traduzirErro(ex)));
    }

    private void falha(String msg) {
        _isLoading.setValue(false);
        _errorMessage.setValue(msg);
    }

    private String traduzirErro(Exception ex) {
        if (ex instanceof FirebaseAuthUserCollisionException) {
            _emailError.setValue("Este e-mail já está cadastrado");
            return "Já existe uma conta com este e-mail";
        }
        if (ex instanceof FirebaseAuthWeakPasswordException) {
            _senhaError.setValue("Senha muito fraca");
            return "Escolha uma senha mais forte";
        }
        if (ex instanceof FirebaseAuthInvalidCredentialsException) {
            _emailError.setValue("E-mail inválido");
            return "Verifique o e-mail informado";
        }
        if (ex instanceof FirebaseNetworkException) {
            return "Sem conexão com a internet";
        }
        return ex.getMessage() != null ? ex.getMessage() : "Erro ao criar conta";
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
