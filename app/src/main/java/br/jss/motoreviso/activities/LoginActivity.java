package br.jss.motoreviso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import br.jss.motoreviso.R;
import br.jss.motoreviso.utils.BiometricHelper;
import br.jss.motoreviso.utils.SecurePreferencesManager;
import br.jss.motoreviso.utils.SystemBarHelper;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private TextInputEditText edtEmail, edtSenha, edtPin;
    private MaterialButton btnEntrar, btnRegistrar, btnEntrarPin, btnBiometria;
    private LinearLayout layoutEmail, layoutPin, layoutBiometria;
    private TextView textBiometriaNaoDisponivel;
    private ProgressBar progressLogin;
    private TabLayout tabLayout;

    private FirebaseAuth auth;
    private SecurePreferencesManager securePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        auth = FirebaseAuth.getInstance();
        securePrefs = new SecurePreferencesManager(this);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && securePrefs.isLoggedIn()) {
            irParaMain();
            return;
        }

        inicializarViews();
        configurarTabs();
    }

    private void inicializarViews() {
        edtEmail = findViewById(R.id.edt_email);
        edtSenha = findViewById(R.id.edt_senha);
        edtPin = findViewById(R.id.edt_pin);
        btnEntrar = findViewById(R.id.btn_entrar);
        btnRegistrar = findViewById(R.id.btn_registrar);
        btnEntrarPin = findViewById(R.id.btn_entrar_pin);
        btnBiometria = findViewById(R.id.btn_biometria);
        layoutEmail = findViewById(R.id.layout_email);
        layoutPin = findViewById(R.id.layout_pin);
        layoutBiometria = findViewById(R.id.layout_biometria);
        textBiometriaNaoDisponivel = findViewById(R.id.text_biometria_nao_disponivel);
        progressLogin = findViewById(R.id.progress_login);
        tabLayout = findViewById(R.id.tab_layout);

        btnEntrar.setOnClickListener(v -> realizarLoginEmail());
        btnRegistrar.setOnClickListener(v -> realizarRegistro());
        btnEntrarPin.setOnClickListener(v -> realizarLoginPin());
        btnBiometria.setOnClickListener(v -> realizarLoginBiometria());
    }

    private void configurarTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("E-mail"));

        if (securePrefs.isPinHabilitado()) {
            tabLayout.addTab(tabLayout.newTab().setText("PIN"));
        }

        if (BiometricHelper.isBiometriaDisponivel(this) && securePrefs.isBiometriaHabilitada()) {
            tabLayout.addTab(tabLayout.newTab().setText("Biometria"));
            iniciarBiometriaAutomatica();
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mostrarSecao(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        mostrarSecao(0);
    }

    private void mostrarSecao(int posicao) {
        layoutEmail.setVisibility(View.GONE);
        layoutPin.setVisibility(View.GONE);
        layoutBiometria.setVisibility(View.GONE);

        TabLayout.Tab tab = tabLayout.getTabAt(posicao);
        if (tab == null) return;
        String label = tab.getText() != null ? tab.getText().toString() : "";

        switch (label) {
            case "E-mail":
                layoutEmail.setVisibility(View.VISIBLE);
                break;
            case "PIN":
                layoutPin.setVisibility(View.VISIBLE);
                break;
            case "Biometria":
                layoutBiometria.setVisibility(View.VISIBLE);
                configurarBiometria();
                break;
        }
    }

    private void configurarBiometria() {
        if (BiometricHelper.isBiometriaDisponivel(this)) {
            btnBiometria.setEnabled(true);
            textBiometriaNaoDisponivel.setVisibility(View.GONE);
        } else {
            btnBiometria.setEnabled(false);
            textBiometriaNaoDisponivel.setVisibility(View.VISIBLE);
        }
    }

    private void iniciarBiometriaAutomatica() {
        if (BiometricHelper.isBiometriaDisponivel(this) && securePrefs.isBiometriaHabilitada()) {
            BiometricHelper.autenticar(this, "Moto Reviso", "Confirme sua identidade",
                    new BiometricHelper.BiometricCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Biometria automática bem-sucedida");
                            concluirLogin();
                        }

                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "Erro na biometria automática: " + message);
                        }

                        @Override
                        public void onFailed() {}
                    });
        }
    }

    private void realizarLoginEmail() {
        String email = edtEmail.getText() != null ? edtEmail.getText().toString().trim() : "";
        String senha = edtSenha.getText() != null ? edtSenha.getText().toString() : "";

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Preencha e-mail e senha", Toast.LENGTH_SHORT).show();
            return;
        }

        setCarregando(true);
        Log.d(TAG, "Tentando login: " + email);

        auth.signInWithEmailAndPassword(email, senha)
                .addOnSuccessListener(authResult -> {
                    setCarregando(false);
                    Log.d(TAG, "Login bem-sucedido: " + email);
                    securePrefs.salvarEmail(email);
                    perguntarSobrePin();
                })
                .addOnFailureListener(e -> {
                    setCarregando(false);
                    Log.e(TAG, "Falha no login", e);
                    Toast.makeText(this, "Login falhou: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void realizarRegistro() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void realizarLoginPin() {
        String pin = edtPin.getText() != null ? edtPin.getText().toString() : "";

        if (pin.length() < 4) {
            Toast.makeText(this, "PIN deve ter ao menos 4 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (securePrefs.verificarPin(pin)) {
            Log.d(TAG, "Login por PIN bem-sucedido");
            concluirLogin();
        } else {
            Log.w(TAG, "PIN incorreto");
            Toast.makeText(this, "PIN incorreto", Toast.LENGTH_SHORT).show();
            edtPin.setText("");
        }
    }

    private void realizarLoginBiometria() {
        BiometricHelper.autenticar(this, "Moto Reviso", "Use sua digital ou rosto",
                new BiometricHelper.BiometricCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Biometria autenticada");
                        concluirLogin();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(LoginActivity.this, "Erro: " + message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed() {
                        Toast.makeText(LoginActivity.this, "Biometria não reconhecida", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void perguntarSobrePin() {
        if (!securePrefs.isPinHabilitado()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Configurar acesso rápido")
                    .setMessage("Deseja configurar um PIN ou biometria para entrar mais rápido nos próximos acessos?")
                    .setPositiveButton("Configurar PIN", (d, w) -> {
                        Intent intent = new Intent(this, SetupPinActivity.class);
                        startActivity(intent);
                        concluirLogin();
                    })
                    .setNegativeButton("Agora não", (d, w) -> concluirLogin())
                    .show();
        } else {
            concluirLogin();
        }
    }

    private void concluirLogin() {
        securePrefs.setLoggedIn(true);
        irParaMain();
    }

    private void irParaMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setCarregando(boolean carregando) {
        progressLogin.setVisibility(carregando ? View.VISIBLE : View.GONE);
        btnEntrar.setEnabled(!carregando);
        btnRegistrar.setEnabled(!carregando);
    }
}
