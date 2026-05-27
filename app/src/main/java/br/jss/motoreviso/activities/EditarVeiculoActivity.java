package br.jss.motoreviso.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Veiculo;
import br.jss.motoreviso.utils.SystemBarHelper;

public class EditarVeiculoActivity extends AppCompatActivity {
    private String veiculoId;
    private EditText edtMarca, edtModelo, edtPlaca, edtKmAtual, edtDescricao;
    private Button btnSalvar, btnCancelar;
    private FirebaseManager firebaseManager;
    private Veiculo veiculoAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_veiculo);

        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        veiculoId = getIntent().getStringExtra("VEICULO_ID");
        if (veiculoId == null) {
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
        inicializarViews();
        carregarVeiculo();

        btnSalvar.setOnClickListener(v -> salvarAlteracoes());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void inicializarViews() {
        edtMarca = findViewById(R.id.edt_marca_editar);
        edtModelo = findViewById(R.id.edt_modelo_editar);
        edtPlaca = findViewById(R.id.edt_placa_editar);
        edtKmAtual = findViewById(R.id.edt_km_atual_editar);
        edtDescricao = findViewById(R.id.edt_descricao_editar);
        btnSalvar = findViewById(R.id.btn_salvar_editar);
        btnCancelar = findViewById(R.id.btn_cancelar_editar);
    }

    private void carregarVeiculo() {
        firebaseManager.obterVeiculo(veiculoId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                veiculoAtual = task.getResult().toObject(Veiculo.class);
                if (veiculoAtual != null) {
                    veiculoAtual.setId(veiculoId);
                    preencherCampos();
                }
            } else {
                Toast.makeText(this, "Erro ao carregar veículo", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void preencherCampos() {
        edtMarca.setText(veiculoAtual.getMarca() != null ? veiculoAtual.getMarca() : "");
        edtModelo.setText(veiculoAtual.getModelo() != null ? veiculoAtual.getModelo() : "");
        edtPlaca.setText(veiculoAtual.getPlaca() != null ? veiculoAtual.getPlaca() : "");
        edtKmAtual.setText(veiculoAtual.getKmAtual() != null ? veiculoAtual.getKmAtual().toString() : "0");
        edtDescricao.setText(veiculoAtual.getDescricao() != null ? veiculoAtual.getDescricao() : "");
    }

    private void salvarAlteracoes() {
        String marca = edtMarca.getText().toString().trim();
        String modelo = edtModelo.getText().toString().trim();
        String placa = edtPlaca.getText().toString().trim();

        if (marca.isEmpty() || modelo.isEmpty() || placa.isEmpty()) {
            Toast.makeText(this, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        veiculoAtual.setMarca(marca);
        veiculoAtual.setModelo(modelo);
        veiculoAtual.setPlaca(placa);

        try {
            veiculoAtual.setKmAtual(Long.parseLong(edtKmAtual.getText().toString()));
        } catch (NumberFormatException e) {
            veiculoAtual.setKmAtual(0L);
        }

        veiculoAtual.setDescricao(edtDescricao.getText().toString());

        firebaseManager.atualizarVeiculo(veiculoId, veiculoAtual)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Veículo atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao atualizar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}