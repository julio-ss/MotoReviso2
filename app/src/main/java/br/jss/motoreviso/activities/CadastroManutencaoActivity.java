package br.jss.motoreviso.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Manutencao;
import br.jss.motoreviso.models.Veiculo;
import br.jss.motoreviso.utils.KeyboardScrollHelper;
import br.jss.motoreviso.utils.SystemBarHelper;

import com.google.android.material.textfield.TextInputEditText;

public class CadastroManutencaoActivity extends AppCompatActivity {
    private static final String TAG = "CadastroManutencaoActivity";

    // Intervalos padrão sugeridos por tipo (em km)
    private static final Map<String, Long> INTERVALO_PADRAO = new HashMap<String, Long>() {{
        put("Troca de Óleo + Filtro",     5000L);
        put("Revisão Geral",              10000L);
        put("Corrente / Relação",         5000L);
        put("Troca de Pneu Dianteiro",    20000L);
        put("Troca de Pneu Traseiro",     15000L);
        put("Freios Dianteiros",          15000L);
        put("Freios Traseiros",           15000L);
        put("Filtro de Ar",               10000L);
        put("Velas / Ignição",            12000L);
        put("Bateria",                    30000L);
        put("Suspensão",                  20000L);
        put("Troca de Fluido de Freio",   20000L);
    }};

    private String veiculoId;
    private String manutencaoId;

    private AutoCompleteTextView acvTipo;
    private TextInputEditText edtData, edtKm, edtProximaKm, edtCusto, edtMecanico, edtPecas, edtNotas;
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
        setupDropdownTipo();

        // Configurar scroll automático para campos quando o teclado abre
        ScrollView scrollView = findViewById(R.id.scroll_view_manutencao);
        KeyboardScrollHelper.setupKeyboardScrollForScrollView(scrollView);

        if (veiculoId == null) {
            layoutSelecionarVeiculo.setVisibility(View.VISIBLE);
            carregarVeiculos();
        }

        if (manutencaoId != null) {
            carregarManutencaoExistente();
        }

        findViewById(R.id.btn_salvar_manutencao).setOnClickListener(v -> salvarManutencao());
        findViewById(R.id.btn_cancelar_manutencao).setOnClickListener(v -> finish());
    }

    private void inicializarViews() {
        acvTipo = findViewById(R.id.acv_tipo_manutencao);
        edtData = findViewById(R.id.edt_data_manutencao);
        edtKm = findViewById(R.id.edt_km_manutencao);
        edtProximaKm = findViewById(R.id.edt_proxima_revisao_km);
        edtCusto = findViewById(R.id.edt_custo);
        edtMecanico = findViewById(R.id.edt_mecanico);
        edtPecas = findViewById(R.id.edt_pecas);
        edtNotas = findViewById(R.id.edt_notas);
        layoutSelecionarVeiculo = findViewById(R.id.layout_selecionar_veiculo);
        spinnerVeiculo = findViewById(R.id.spinner_veiculo);
        progressCarregandoVeiculos = findViewById(R.id.progress_carregando_veiculos);

        edtData.setOnClickListener(v -> mostrarDatePicker());
    }

    private void setupDropdownTipo() {
        String[] tipos = getResources().getStringArray(R.array.tipos_manutencao);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, tipos);
        acvTipo.setAdapter(adapter);

        // Ao selecionar tipo, sugere automaticamente a próxima troca em km
        acvTipo.setOnItemClickListener((parent, view, position, id) -> {
            String tipo = (String) parent.getItemAtPosition(position);
            sugerirProximaKm(tipo);
        });
    }

    private void sugerirProximaKm(String tipo) {
        // Só sugere se o campo ainda estiver vazio
        if (edtProximaKm.getText() != null && !edtProximaKm.getText().toString().isEmpty()) return;

        Long interval = INTERVALO_PADRAO.get(tipo);
        if (interval == null) return;

        String kmStr = edtKm.getText() != null ? edtKm.getText().toString().trim() : "";
        if (!kmStr.isEmpty()) {
            try {
                long kmAtual = Long.parseLong(kmStr);
                edtProximaKm.setText(String.valueOf(kmAtual + interval));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void mostrarDatePicker() {
        // Usa AlertDialog com DatePicker view para melhor controle de espaço e visibilidade dos botões
        android.widget.DatePicker datePicker = new android.widget.DatePicker(this);
        datePicker.init(calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                        null);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Data da Manutenção")
                .setView(datePicker)
                .setPositiveButton("OK", (dialog, which) -> {
                    calendar.set(datePicker.getYear(),
                                datePicker.getMonth(),
                                datePicker.getDayOfMonth());
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
                    edtData.setText(sdf.format(calendar.getTime()));
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void carregarVeiculos() {
        progressCarregandoVeiculos.setVisibility(View.VISIBLE);

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
                        nomes.add(v.getMarca() + " " + v.getModelo() + " — " + v.getPlaca());
                    }
                }

                if (veiculos.isEmpty()) {
                    Toast.makeText(this, "Cadastre um veículo antes de registrar manutenção.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, nomes);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerVeiculo.setAdapter(adapter);
            } else {
                Toast.makeText(this, "Erro ao carregar veículos", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void carregarManutencaoExistente() {
        firebaseManager.obterManutencao(manutencaoId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Manutencao m = task.getResult().toObject(Manutencao.class);
                if (m != null) preencherFormulario(m);
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
        if (m.getTipo() != null) acvTipo.setText(m.getTipo(), false);
        if (m.getProximaRevisaoKm() != null) edtProximaKm.setText(String.valueOf(m.getProximaRevisaoKm()));
        if (m.getCusto() != null) edtCusto.setText(String.valueOf(m.getCusto()));
        if (m.getMecanico() != null) edtMecanico.setText(m.getMecanico());
        if (m.getNotas() != null) edtNotas.setText(m.getNotas());
        if (m.getPecasTrocadas() != null && !m.getPecasTrocadas().isEmpty()) {
            edtPecas.setText(String.join(", ", m.getPecasTrocadas()));
        }
    }

    private void salvarManutencao() {
        String tipo = acvTipo.getText().toString().trim();
        String dataStr = edtData.getText().toString().trim();
        String kmStr = edtKm.getText().toString().trim();

        if (tipo.isEmpty() || dataStr.isEmpty() || kmStr.isEmpty()) {
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

        long kmRevisao;
        try {
            kmRevisao = Long.parseLong(kmStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Formato de KM inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        Manutencao manutencao = new Manutencao(idVeiculo);
        manutencao.setDataRevisao(calendar.getTimeInMillis());
        manutencao.setKmRevisao(kmRevisao);
        manutencao.setTipo(tipo);

        // Próxima troca em KM
        String proximaKmStr = edtProximaKm.getText() != null ? edtProximaKm.getText().toString().trim() : "";
        if (!proximaKmStr.isEmpty()) {
            try {
                manutencao.setProximaRevisaoKm(Long.parseLong(proximaKmStr));
            } catch (NumberFormatException ignored) {}
        } else {
            // Calcula automaticamente se houver intervalo padrão
            Long interval = INTERVALO_PADRAO.get(tipo);
            if (interval != null) {
                manutencao.setProximaRevisaoKm(kmRevisao + interval);
            }
        }

        String custoStr = edtCusto.getText() != null ? edtCusto.getText().toString().trim() : "";
        if (!custoStr.isEmpty()) {
            try {
                manutencao.setCusto(Double.parseDouble(custoStr.replace(",", ".")));
            } catch (NumberFormatException e) {
                manutencao.setCusto(0.0);
            }
        }

        manutencao.setMecanico(edtMecanico.getText() != null ? edtMecanico.getText().toString().trim() : "");
        manutencao.setNotas(edtNotas.getText() != null ? edtNotas.getText().toString().trim() : "");

        String pecasStr = edtPecas.getText() != null ? edtPecas.getText().toString().trim() : "";
        if (!pecasStr.isEmpty()) {
            List<String> pecas = new ArrayList<>();
            for (String p : pecasStr.split(",")) {
                String t = p.trim();
                if (!t.isEmpty()) pecas.add(t);
            }
            manutencao.setPecasTrocadas(pecas);
        }

        findViewById(R.id.btn_salvar_manutencao).setEnabled(false);

        final String finalVeiculoId = idVeiculo;

        if (manutencaoId != null) {
            manutencao.setId(manutencaoId);
            firebaseManager.atualizarManutencao(manutencaoId, manutencao)
                    .addOnSuccessListener(v -> {
                        atualizarKmVeiculo(finalVeiculoId, kmRevisao);
                        Toast.makeText(this, "Manutenção atualizada!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        findViewById(R.id.btn_salvar_manutencao).setEnabled(true);
                        Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            firebaseManager.adicionarManutencao(manutencao)
                    .addOnSuccessListener(ref -> {
                        atualizarKmVeiculo(finalVeiculoId, kmRevisao);
                        Toast.makeText(this, getString(R.string.manutencao_salva), Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        findViewById(R.id.btn_salvar_manutencao).setEnabled(true);
                        Toast.makeText(this, "Erro ao salvar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Atualiza o kmAtual do veículo se o KM da manutenção for maior que o atual.
     * Também atualiza kmTroca para recalcular corretamente as revisões pendentes.
     */
    private void atualizarKmVeiculo(String idVeiculo, long kmManutencao) {
        firebaseManager.obterVeiculo(idVeiculo).addOnSuccessListener(doc -> {
            if (doc == null || !doc.exists()) return;
            Veiculo v = doc.toObject(Veiculo.class);
            if (v == null) return;
            v.setId(doc.getId());

            boolean atualizado = false;

            // Atualiza kmAtual se a manutenção registrou um KM maior
            if (v.getKmAtual() == null || kmManutencao > v.getKmAtual()) {
                v.setKmAtual(kmManutencao);
                atualizado = true;
            }

            // Atualiza kmTroca (referência para cálculo de próxima revisão geral)
            String tipo = acvTipo.getText().toString().trim();
            if (tipo.contains("Óleo") || tipo.contains("Revisão Geral")) {
                v.setKmTroca(kmManutencao);
                atualizado = true;
            }

            if (atualizado) {
                firebaseManager.atualizarVeiculo(v)
                        .addOnFailureListener(e -> Log.e(TAG, "Erro ao atualizar KM do veículo", e));
            }
        });
    }
}
