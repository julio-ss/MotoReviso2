package br.jss.motoreviso.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Manutencao;
import br.jss.motoreviso.models.Veiculo;
import br.jss.motoreviso.utils.SystemBarHelper;

public class CadastroManutencaoActivity extends AppCompatActivity {
    private static final String TAG = "CadastroManutencaoActivity";

    private String veiculoId;
    private String manutencaoId;
    private EditText edtData, edtKm, edtTipo, edtCusto, edtMecanico, edtPecas, edtNotas;
    private Button btnSalvar, btnCancelar;
    private LinearLayout layoutSelecionarVeiculo;
    private Spinner spinnerVeiculo;
    private ProgressBar progressCarregandoVeiculos;
    private FirebaseManager firebaseManager;
    private Calendar calendar = Calendar.getInstance();

    private List<Veiculo> veiculos = new ArrayList<>();
    private List<String> veiculoIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_manutencao);

        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        veiculoId = getIntent().getStringExtra("VEICULO_ID");
        manutencaoId = getIntent().getStringExtra("MANUTENCAO_ID");

        firebaseManager = FirebaseManager.getInstance();
        inicializarViews();

        if (veiculoId == null) {
            layoutSelecionarVeiculo.setVisibility(View.VISIBLE);
            carregarVeiculos();
        }

        if (manutencaoId != null) {
            carregarManutencaoExistente();
        }

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
        layoutSelecionarVeiculo = findViewById(R.id.layout_selecionar_veiculo);
        spinnerVeiculo = findViewById(R.id.spinner_veiculo);
        progressCarregandoVeiculos = findViewById(R.id.progress_carregando_veiculos);

        edtData.setOnClickListener(v -> mostrarDatePicker());
        edtData.setFocusable(false);
    }

    private void mostrarDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, day) -> {
                    calendar.set(year, month, day);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
                    edtData.setText(sdf.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void carregarVeiculos() {
        progressCarregandoVeiculos.setVisibility(View.VISIBLE);
        Log.d(TAG, "Carregando veículos para seleção");

        firebaseManager.obterTodosVeiculos().addOnCompleteListener(task -> {
            progressCarregandoVeiculos.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                veiculos.clear();
                veiculoIds.clear();
                List<String> nomes = new ArrayList<>();

                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Veiculo v = doc.toObject(Veiculo.class);
                    if (v != null) {
                        v.setId(doc.getId());
                        veiculos.add(v);
                        veiculoIds.add(doc.getId());
                        nomes.add(v.getMarca() + " " + v.getModelo() + " - " + v.getPlaca());
                    }
                }

                if (veiculos.isEmpty()) {
                    Toast.makeText(this, "Nenhum veículo cadastrado. Cadastre um veículo primeiro.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, nomes);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerVeiculo.setAdapter(adapter);
            } else {
                Log.e(TAG, "Erro ao carregar veículos", task.getException());
                Toast.makeText(this, "Erro ao carregar veículos", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void carregarManutencaoExistente() {
        Log.d(TAG, "Carregando manutenção existente: " + manutencaoId);
        firebaseManager.obterManutencao(manutencaoId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Manutencao m = task.getResult().toObject(Manutencao.class);
                if (m != null) {
                    preencherFormulario(m);
                }
            } else {
                Log.e(TAG, "Erro ao carregar manutenção", task.getException());
            }
        });
    }

    private void preencherFormulario(Manutencao m) {
        if (m.getDataRevisao() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
            edtData.setText(sdf.format(m.getDataRevisao()));
            calendar.setTimeInMillis(m.getDataRevisao());
        }
        if (m.getKmRevisao() != null) edtKm.setText(String.valueOf(m.getKmRevisao()));
        if (m.getTipo() != null) edtTipo.setText(m.getTipo());
        if (m.getCusto() != null) edtCusto.setText(String.valueOf(m.getCusto()));
        if (m.getMecanico() != null) edtMecanico.setText(m.getMecanico());
        if (m.getNotas() != null) edtNotas.setText(m.getNotas());
        if (m.getPecasTrocadas() != null && !m.getPecasTrocadas().isEmpty()) {
            edtPecas.setText(String.join(", ", m.getPecasTrocadas()));
        }
    }

    private void salvarManutencao() {
        String dataStr = edtData.getText().toString().trim();
        String kmStr = edtKm.getText().toString().trim();
        String tipo = edtTipo.getText().toString().trim();

        if (dataStr.isEmpty() || kmStr.isEmpty() || tipo.isEmpty()) {
            Toast.makeText(this, getString(R.string.preencha_obrigatorios), Toast.LENGTH_SHORT).show();
            return;
        }

        String idVeiculo = veiculoId;
        if (idVeiculo == null) {
            int pos = spinnerVeiculo.getSelectedItemPosition();
            if (pos < 0 || pos >= veiculoIds.size()) {
                Toast.makeText(this, "Selecione um veículo", Toast.LENGTH_SHORT).show();
                return;
            }
            idVeiculo = veiculoIds.get(pos);
        }

        Manutencao manutencao = new Manutencao(idVeiculo);

        try {
            manutencao.setDataRevisao(calendar.getTimeInMillis());
            manutencao.setKmRevisao(Long.parseLong(kmStr));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Formato de KM inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        manutencao.setTipo(tipo);

        String custoStr = edtCusto.getText().toString().trim();
        if (!custoStr.isEmpty()) {
            try {
                manutencao.setCusto(Double.parseDouble(custoStr.replace(",", ".")));
            } catch (NumberFormatException e) {
                manutencao.setCusto(0.0);
            }
        }

        manutencao.setMecanico(edtMecanico.getText().toString().trim());
        manutencao.setNotas(edtNotas.getText().toString().trim());

        String pecasStr = edtPecas.getText().toString().trim();
        if (!pecasStr.isEmpty()) {
            List<String> pecas = new ArrayList<>();
            for (String p : pecasStr.split(",")) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) pecas.add(trimmed);
            }
            manutencao.setPecasTrocadas(pecas);
        }

        btnSalvar.setEnabled(false);

        if (manutencaoId != null) {
            manutencao.setId(manutencaoId);
            // Update existing
            firebaseManager.atualizarManutencao(manutencaoId, manutencao)
                    .addOnSuccessListener(v -> {
                        Log.d(TAG, "Manutenção atualizada: " + manutencaoId);
                        Toast.makeText(this, "Manutenção atualizada com sucesso!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erro ao atualizar manutenção", e);
                        btnSalvar.setEnabled(true);
                        Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            firebaseManager.adicionarManutencao(manutencao)
                    .addOnSuccessListener(ref -> {
                        Log.d(TAG, "Manutenção criada: " + ref.getId());
                        Toast.makeText(this, getString(R.string.manutencao_salva), Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erro ao salvar manutenção", e);
                        btnSalvar.setEnabled(true);
                        Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
