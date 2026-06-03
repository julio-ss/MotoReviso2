package br.jss.motoreviso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import br.jss.motoreviso.R;
import br.jss.motoreviso.viewmodels.RegisterViewModel;

public class RegisterActivity extends AppCompatActivity {

    private RegisterViewModel viewModel;

    private TextInputLayout tilNome, tilApelido, tilEmail, tilTelefone, tilCidade, tilSenha, tilConfirmarSenha;
    private TextInputEditText edtNome, edtApelido, edtEmail, edtTelefone, edtCidade, edtSenha, edtConfirmarSenha;
    private TextView textNomeError, textApelidoError, textEmailError, textTelefoneError, textCidadeError, textSenhaError, textConfirmarSenhaError;
    private TextView textErrorMessage, textLoginLink;
    private MaterialButton btnCadastrar;
    private ProgressBar progressLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        inicializarViews();
        configurarListeners();
        observarViewModel();
    }

    private void inicializarViews() {
        tilNome = findViewById(R.id.til_nome);
        tilApelido = findViewById(R.id.til_apelido);
        tilEmail = findViewById(R.id.til_email);
        tilTelefone = findViewById(R.id.til_telefone);
        tilCidade = findViewById(R.id.til_cidade);
        tilSenha = findViewById(R.id.til_senha);
        tilConfirmarSenha = findViewById(R.id.til_confirmar_senha);

        edtNome = findViewById(R.id.edt_nome);
        edtApelido = findViewById(R.id.edt_apelido);
        edtEmail = findViewById(R.id.edt_email);
        edtTelefone = findViewById(R.id.edt_telefone);
        edtCidade = findViewById(R.id.edt_cidade);
        edtSenha = findViewById(R.id.edt_senha);
        edtConfirmarSenha = findViewById(R.id.edt_confirmar_senha);

        textNomeError = findViewById(R.id.text_nome_error);
        textApelidoError = findViewById(R.id.text_apelido_error);
        textEmailError = findViewById(R.id.text_email_error);
        textTelefoneError = findViewById(R.id.text_telefone_error);
        textCidadeError = findViewById(R.id.text_cidade_error);
        textSenhaError = findViewById(R.id.text_senha_error);
        textConfirmarSenhaError = findViewById(R.id.text_confirmar_senha_error);

        textErrorMessage = findViewById(R.id.text_error_message);
        textLoginLink = findViewById(R.id.text_login_link);
        btnCadastrar = findViewById(R.id.btn_cadastrar);
        progressLoading = findViewById(R.id.progress_loading);
    }

    private void configurarListeners() {
        // Máscara de telefone
        edtTelefone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String apenasNumeros = s.toString().replaceAll("[^0-9]", "");
                if (apenasNumeros.length() > before) {
                    edtTelefone.removeTextChangedListener(this);
                    edtTelefone.setText(viewModel.formatarTelefone(apenasNumeros));
                    edtTelefone.setSelection(edtTelefone.getText().length());
                    edtTelefone.addTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Limpar erros ao digitar
        edtNome.addTextChangedListener(createTextWatcher(textNomeError));
        edtApelido.addTextChangedListener(createTextWatcher(textApelidoError));
        edtEmail.addTextChangedListener(createTextWatcher(textEmailError));
        edtTelefone.addTextChangedListener(createTextWatcher(textTelefoneError));
        edtCidade.addTextChangedListener(createTextWatcher(textCidadeError));
        edtSenha.addTextChangedListener(createTextWatcher(textSenhaError));
        edtConfirmarSenha.addTextChangedListener(createTextWatcher(textConfirmarSenhaError));

        btnCadastrar.setOnClickListener(v -> realizarCadastro());
        textLoginLink.setOnClickListener(v -> irParaLogin());
    }

    private TextWatcher createTextWatcher(TextView errorView) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                errorView.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
    }

    private void observarViewModel() {
        viewModel.nomeError.observe(this, error -> atualizarErro(textNomeError, error));
        viewModel.apelidoError.observe(this, error -> atualizarErro(textApelidoError, error));
        viewModel.emailError.observe(this, error -> atualizarErro(textEmailError, error));
        viewModel.telefoneError.observe(this, error -> atualizarErro(textTelefoneError, error));
        viewModel.cidadeError.observe(this, error -> atualizarErro(textCidadeError, error));
        viewModel.senhaError.observe(this, error -> atualizarErro(textSenhaError, error));
        viewModel.confirmarSenhaError.observe(this, error -> atualizarErro(textConfirmarSenhaError, error));

        viewModel.isLoading.observe(this, isLoading -> {
            progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnCadastrar.setEnabled(!isLoading);
        });

        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                textErrorMessage.setText(error);
                textErrorMessage.setVisibility(View.VISIBLE);
            } else {
                textErrorMessage.setVisibility(View.GONE);
            }
        });

        viewModel.successMessage.observe(this, success -> {
            if (success != null && !success.isEmpty()) {
                irParaMainActivity();
            }
        });
    }

    private void atualizarErro(TextView errorView, String error) {
        if (error != null && !error.isEmpty()) {
            errorView.setText(error);
            errorView.setVisibility(View.VISIBLE);
        } else {
            errorView.setVisibility(View.GONE);
        }
    }

    private void realizarCadastro() {
        String nome = edtNome.getText().toString();
        String apelido = edtApelido.getText().toString();
        String email = edtEmail.getText().toString();
        String telefone = edtTelefone.getText().toString();
        String cidade = edtCidade.getText().toString();
        String senha = edtSenha.getText().toString();
        String confirmarSenha = edtConfirmarSenha.getText().toString();

        // Validar
        if (viewModel.validarFormulario(nome, apelido, email, telefone, cidade, senha, confirmarSenha)) {
            textErrorMessage.setVisibility(View.GONE);
            // Registrar
            viewModel.registrar(nome, apelido, email, telefone, cidade, senha);
        }
    }

    private void irParaMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void irParaLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
