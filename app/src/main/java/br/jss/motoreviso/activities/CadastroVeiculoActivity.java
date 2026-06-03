package br.jss.motoreviso.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.transition.TransitionManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import br.jss.motoreviso.R;
import br.jss.motoreviso.config.VehicleTypeConfig;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Veiculo;
import br.jss.motoreviso.utils.SystemBarHelper;

public class CadastroVeiculoActivity extends AppCompatActivity {

    // Dropdowns
    private AutoCompleteTextView acvTipoVeiculo;
    private AutoCompleteTextView acvAlimentacao;

    // Campos básicos
    private TextInputEditText edtMarca, edtModelo, edtAno, edtPlaca, edtChassis;

    // Campos motor convencional
    private TextInputEditText edtMotor, edtCambio, edtPotencia, edtTorque, edtCilindrada;

    // Campos alimentação
    private TextInputEditText edtTanque;

    // Campos elétrico
    private TextInputEditText edtBateria, edtAutonomia, edtTempoRecarga;

    // Campos comuns
    private TextInputEditText edtPeso, edtDimensoes, edtKmAtual, edtIntervalo, edtDescricao;

    // Seções dinâmicas
    private LinearLayout contentLayout;
    private LinearLayout sectionMotor;
    private LinearLayout sectionAlimentacao;
    private LinearLayout sectionEletrico;
    private TextInputLayout tilTanque;

    // Imagem
    private ImageView imgVeiculo;
    private Uri imagemUri;
    private ActivityResultLauncher<Intent> galeriaLauncher;

    // Botões
    private MaterialButton btnSalvar, btnCancelar, btnSelecionar;

    private FirebaseManager firebaseManager;
    private VehicleTypeConfig.VehicleType tipoSelecionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_veiculo);
        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        firebaseManager = FirebaseManager.getInstance();
        inicializarViews();
        setupDropdownTipo();
        setupDropdownAlimentacao();
        setupGaleriaLauncher();

        btnSalvar.setOnClickListener(v -> salvarVeiculo());
        btnCancelar.setOnClickListener(v -> finish());
        btnSelecionar.setOnClickListener(v -> abrirGaleria());
    }

    private void inicializarViews() {
        contentLayout = findViewById(R.id.content_layout);
        imgVeiculo = findViewById(R.id.img_veiculo);

        acvTipoVeiculo = findViewById(R.id.acv_tipo_veiculo);
        acvAlimentacao = findViewById(R.id.acv_alimentacao);

        edtMarca = findViewById(R.id.edt_marca);
        edtModelo = findViewById(R.id.edt_modelo);
        edtAno = findViewById(R.id.edt_ano);
        edtPlaca = findViewById(R.id.edt_placa);
        edtChassis = findViewById(R.id.edt_chassis);

        edtMotor = findViewById(R.id.edt_motor);
        edtCambio = findViewById(R.id.edt_cambio);
        edtPotencia = findViewById(R.id.edt_potencia);
        edtTorque = findViewById(R.id.edt_torque);
        edtCilindrada = findViewById(R.id.edt_cilindrada);

        edtTanque = findViewById(R.id.edt_tanque);
        tilTanque = findViewById(R.id.til_tanque);

        edtBateria = findViewById(R.id.edt_bateria);
        edtAutonomia = findViewById(R.id.edt_autonomia);
        edtTempoRecarga = findViewById(R.id.edt_tempo_recarga);

        edtPeso = findViewById(R.id.edt_peso);
        edtDimensoes = findViewById(R.id.edt_dimensoes);
        edtKmAtual = findViewById(R.id.edt_km_atual);
        edtIntervalo = findViewById(R.id.edt_intervalo);
        edtDescricao = findViewById(R.id.edt_descricao);

        sectionMotor = findViewById(R.id.section_motor);
        sectionAlimentacao = findViewById(R.id.section_alimentacao);
        sectionEletrico = findViewById(R.id.section_eletrico);

        btnSalvar = findViewById(R.id.btn_salvar);
        btnCancelar = findViewById(R.id.btn_cancelar);
        btnSelecionar = findViewById(R.id.btn_selecionar_imagem);
    }

    private void setupDropdownTipo() {
        List<String> nomes = new ArrayList<>();
        for (VehicleTypeConfig.VehicleType t : VehicleTypeConfig.TODOS) {
            nomes.add(t.nome);
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                nomes
        );
        acvTipoVeiculo.setAdapter(adapter);
        acvTipoVeiculo.setOnItemClickListener((parent, view, position, id) -> {
            String nome = (String) parent.getItemAtPosition(position);
            tipoSelecionado = VehicleTypeConfig.findByName(nome);
            if (tipoSelecionado != null) {
                atualizarSecoesDinamicas(tipoSelecionado);
            }
        });
    }

    private void setupDropdownAlimentacao() {
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                VehicleTypeConfig.TIPOS_ALIMENTACAO
        );
        acvAlimentacao.setAdapter(adapter);
    }

    private void atualizarSecoesDinamicas(VehicleTypeConfig.VehicleType tipo) {
        TransitionManager.beginDelayedTransition(contentLayout);

        boolean showMotorConvencional = tipo.possuiMotor && !tipo.isEletrico;
        boolean showAlimentacao = tipo.possuiMotor;
        boolean showEletrico = tipo.isEletrico;

        sectionMotor.setVisibility(showMotorConvencional ? View.VISIBLE : View.GONE);
        sectionAlimentacao.setVisibility(showAlimentacao ? View.VISIBLE : View.GONE);
        tilTanque.setVisibility(showMotorConvencional ? View.VISIBLE : View.GONE);
        sectionEletrico.setVisibility(showEletrico ? View.VISIBLE : View.GONE);

        if (showEletrico) {
            acvAlimentacao.setText("Elétrico", false);
        } else if (showAlimentacao && acvAlimentacao.getText().toString().equals("Elétrico")) {
            acvAlimentacao.setText("", false);
        }
    }

    private void setupGaleriaLauncher() {
        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imagemUri = result.getData().getData();
                        if (imagemUri != null) {
                            getContentResolver().takePersistableUriPermission(
                                    imagemUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            imgVeiculo.setImageURI(imagemUri);
                            Toast.makeText(this, "Imagem selecionada", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        galeriaLauncher.launch(intent);
    }

    private void salvarVeiculo() {
        String tipoNome = acvTipoVeiculo.getText().toString().trim();
        String marca = edtMarca.getText().toString().trim();
        String modelo = edtModelo.getText().toString().trim();
        String placa = edtPlaca.getText().toString().trim();

        if (tipoNome.isEmpty() || marca.isEmpty() || modelo.isEmpty()) {
            Toast.makeText(this, getString(R.string.preencha_obrigatorios), Toast.LENGTH_SHORT).show();
            return;
        }

        Veiculo veiculo = new Veiculo(tipoNome, marca, modelo, placa);
        veiculo.setAno(getLongValue(edtAno));
        veiculo.setChassis(edtChassis.getText().toString().trim());
        veiculo.setPeso(getLongValue(edtPeso));
        veiculo.setDimensoes(edtDimensoes.getText().toString().trim());
        veiculo.setKmAtual(getLongValue(edtKmAtual));
        veiculo.setKmTroca(getLongValue(edtKmAtual));
        veiculo.setIntervaloRevisao(getLongValue(edtIntervalo));
        if (veiculo.getIntervaloRevisao() == null) {
            veiculo.setIntervaloRevisao(5000L);
        }
        veiculo.setDescricao(edtDescricao.getText().toString().trim());
        veiculo.setDataUltimaRevisao(System.currentTimeMillis());

        if (tipoSelecionado != null) {
            veiculo.setPossuiMotorizacao(tipoSelecionado.possuiMotor);

            if (tipoSelecionado.possuiMotor && !tipoSelecionado.isEletrico) {
                veiculo.setMotor(edtMotor.getText().toString().trim());
                veiculo.setCambio(edtCambio.getText().toString().trim());
                veiculo.setPotencia(getLongValue(edtPotencia));
                veiculo.setTorque(edtTorque.getText().toString().trim());
                veiculo.setCilindrada(getLongValue(edtCilindrada));
                veiculo.setTanque(getLongValue(edtTanque));
            }

            if (tipoSelecionado.possuiMotor) {
                String alimentacao = acvAlimentacao.getText().toString().trim();
                veiculo.setTipoAlimentacao(alimentacao);
                veiculo.setCombustivel(alimentacao); // compatibilidade retroativa
            }

            if (tipoSelecionado.isEletrico) {
                veiculo.setCapacidadeBateria(getLongValue(edtBateria));
                veiculo.setAutonomia(getLongValue(edtAutonomia));
                veiculo.setTempoRecarga(edtTempoRecarga.getText().toString().trim());
            }
        }

        if (imagemUri != null) {
            veiculo.setUrlImagemPrincipal(imagemUri.toString());
        }

        firebaseManager.adicionarVeiculo(veiculo)
                .addOnSuccessListener(documentReference -> {
                    veiculo.setId(documentReference.getId());
                    Toast.makeText(this, getString(R.string.veiculo_salvo), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                    Toast.makeText(this, "Erro ao cadastrar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private Long getLongValue(TextInputEditText edt) {
        String valor = edt.getText().toString().trim();
        if (valor.isEmpty()) return null;
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
