package br.jss.motoreviso.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Manutencao;

public class CadastroManutencaoActivity extends AppCompatActivity {
    private String veiculoId;
    private EditText edtData, edtKm, edtTipo, edtCusto, edtMecanico, edtPecas, edtNotas;
    private Button btnSalvar, btnCancelar;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_manutencao);

        veiculoId = getIntent().getStringExtra("VEICULO_ID");
        if (veiculoId == null) {
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
        inicializarViews();

        btnSalvar.setOnClickListener(v -> salvarManutencao());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void inicializarViews() {
        edtData = findViewById(R.id.edt_data_manutencao);
        edtKm = findViewById(R.id.edt_km_manutencao);
        edtTipo = findViewById(R.id.edt_tipo_manutencao);
        edtCusto = findViewById(R.id.edt_custo);
        edtMecanico = findViewById(R.id.edt_mecanico);
        edtPecas = findViewById(R.id.edt_pecas);
        edtNotas = findViewById(R.id.edt_notas);
        btnSalvar = findViewById(R.id.btn_salvar_manutencao);
        btnCancelar = findViewById(R.id.btn_cancelar_manutencao);
    }

    private void salvarManutencao() {
        String dataStr = edtData.getText().toString().trim();
        String kmStr = edtKm.getText().toString().trim();
        String tipo = edtTipo.getText().toString().trim();

        if (dataStr.isEmpty() || kmStr.isEmpty() || tipo.isEmpty()) {
            Toast.makeText(this, "Preencha os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        Manutencao manutencao = new Manutencao(veiculoId);

        try {
            // Assumindo formato DD/MM/YYYY
            String[] dataParts = dataStr.split("/");
            if (dataParts.length == 3) {
                int dia = Integer.parseInt(dataParts[0]);
                int mes = Integer.parseInt(dataParts[1]) - 1;
                int ano = Integer.parseInt(dataParts[2]);
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.set(ano, mes, dia, 0, 0, 0);
                manutencao.setDataRevisao(calendar.getTimeInMillis());
            }
            manutencao.setKmRevisao(Long.parseLong(kmStr));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Formato inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        manutencao.setTipo(tipo);

        String custoStr = edtCusto.getText().toString().trim();
        if (!custoStr.isEmpty()) {
            try {
                manutencao.setCusto(Double.parseDouble(custoStr));
            } catch (NumberFormatException e) {
                manutencao.setCusto(0.0);
            }
        }

        manutencao.setMecanico(edtMecanico.getText().toString());
        manutencao.setNotas(edtNotas.getText().toString());

        firebaseManager.adicionarManutencao(manutencao)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Manutenção registrada com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}