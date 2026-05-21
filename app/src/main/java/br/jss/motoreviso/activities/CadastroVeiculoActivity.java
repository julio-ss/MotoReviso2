package br.jss.motoreviso.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Veiculo;

public class CadastroVeiculoActivity extends AppCompatActivity {
    private RadioGroup radioGroupTipo;
    private EditText edtMarca, edtModelo, edtAno, edtPlaca, edtChassis, edtMotor;
    private EditText edtCombustivel, edtCambio, edtPotencia, edtTorque, edtCilindrada;
    private EditText edtPeso, edtDimensoes, edtTanque, edtKmAtual, edtIntervalo, edtDescricao;
    private ImageView imgVeiculo;
    private Button btnSalvar, btnCancelar, btnSelecionar;
    private ScrollView scrollView;
    private FirebaseManager firebaseManager;
    private Uri imagemUri;
    private ActivityResultLauncher<Intent> galeriaLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_veiculo);

        firebaseManager = FirebaseManager.getInstance();
        inicializarViews();
        setupGaleriaLauncher();

        btnSalvar.setOnClickListener(v -> salvarVeiculo());
        btnCancelar.setOnClickListener(v -> finish());
        btnSelecionar.setOnClickListener(v -> abrirGaleria());
    }

    private void inicializarViews() {
        scrollView = findViewById(R.id.scroll_view);
        radioGroupTipo = findViewById(R.id.radio_group_tipo);
        imgVeiculo = findViewById(R.id.img_veiculo);

        edtMarca = findViewById(R.id.edt_marca);
        edtModelo = findViewById(R.id.edt_modelo);
        edtAno = findViewById(R.id.edt_ano);
        edtPlaca = findViewById(R.id.edt_placa);
        edtChassis = findViewById(R.id.edt_chassis);
        edtMotor = findViewById(R.id.edt_motor);
        edtCombustivel = findViewById(R.id.edt_combustivel);
        edtCambio = findViewById(R.id.edt_cambio);
        edtPotencia = findViewById(R.id.edt_potencia);
        edtTorque = findViewById(R.id.edt_torque);
        edtCilindrada = findViewById(R.id.edt_cilindrada);
        edtPeso = findViewById(R.id.edt_peso);
        edtDimensoes = findViewById(R.id.edt_dimensoes);
        edtTanque = findViewById(R.id.edt_tanque);
        edtKmAtual = findViewById(R.id.edt_km_atual);
        edtIntervalo = findViewById(R.id.edt_intervalo);
        edtDescricao = findViewById(R.id.edt_descricao);

        btnSalvar = findViewById(R.id.btn_salvar);
        btnCancelar = findViewById(R.id.btn_cancelar);
        btnSelecionar = findViewById(R.id.btn_selecionar_imagem);
    }

    private void setupGaleriaLauncher() {
        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imagemUri = result.getData().getData();
                        if (imagemUri != null) {
                            imgVeiculo.setImageURI(imagemUri);
                            Toast.makeText(this, "Imagem selecionada", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galeriaLauncher.launch(intent);
    }

    private void salvarVeiculo() {
        String tipo = ((RadioButton) findViewById(radioGroupTipo.getCheckedRadioButtonId())).getText().toString();
        String marca = edtMarca.getText().toString().trim();
        String modelo = edtModelo.getText().toString().trim();
        String placa = edtPlaca.getText().toString().trim();

        if (marca.isEmpty() || modelo.isEmpty() || placa.isEmpty()) {
            Toast.makeText(this, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        Veiculo veiculo = new Veiculo(tipo, marca, modelo, placa);
        veiculo.setAno(getLongValue(edtAno));
        veiculo.setChassis(edtChassis.getText().toString());
        veiculo.setMotor(edtMotor.getText().toString());
        veiculo.setCombustivel(edtCombustivel.getText().toString());
        veiculo.setCambio(edtCambio.getText().toString());
        veiculo.setPotencia(getLongValue(edtPotencia));
        veiculo.setTorque(edtTorque.getText().toString());
        veiculo.setCilindrada(getLongValue(edtCilindrada));
        veiculo.setPeso(getLongValue(edtPeso));
        veiculo.setDimensoes(edtDimensoes.getText().toString());
        veiculo.setTanque(getLongValue(edtTanque));
        veiculo.setKmAtual(getLongValue(edtKmAtual));
        veiculo.setKmTroca(getLongValue(edtKmAtual));
        veiculo.setIntervaloRevisao(getLongValue(edtIntervalo));
        if (veiculo.getIntervaloRevisao() == null) {
            veiculo.setIntervaloRevisao(5000L);
        }
        veiculo.setDescricao(edtDescricao.getText().toString());
        veiculo.setDataUltimaRevisao(System.currentTimeMillis());

        if (imagemUri != null) {
            firebaseManager.uploadImagemVeiculo(imagemUri, "temp_" + System.currentTimeMillis(), new FirebaseManager.OnUploadCompleteListener() {
                @Override
                public void onUploadComplete(String downloadUrl) {
                    veiculo.setUrlImagemPrincipal(downloadUrl);
                    salvarVeiculoNoFirebase(veiculo);
                }

                @Override
                public void onUploadFailed(Exception exception) {
                    Toast.makeText(CadastroVeiculoActivity.this, "Erro ao fazer upload", Toast.LENGTH_SHORT).show();
                    salvarVeiculoNoFirebase(veiculo);
                }
            });
        } else {
            salvarVeiculoNoFirebase(veiculo);
        }
    }

    private void salvarVeiculoNoFirebase(Veiculo veiculo) {
        firebaseManager.adicionarVeiculo(veiculo)
                .addOnSuccessListener(documentReference -> {
                    veiculo.setId(documentReference.getId());
                    Toast.makeText(this, "Veículo cadastrado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao cadastrar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private Long getLongValue(EditText edt) {
        String valor = edt.getText().toString().trim();
        if (valor.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}