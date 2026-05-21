package br.jss.motoreviso.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Manutencao;
import br.jss.motoreviso.utils.ManutencaoFormatter;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DetalheManutencaoActivity extends AppCompatActivity {
    private static final String TAG = "DetalheManutencaoActivity";

    private ScrollView scrollView;
    private TextView textData;
    private TextView textKm;
    private TextView textTipo;
    private TextView textCusto;
    private TextView textMecanico;
    private TextView textDescricao;
    private TextView textPecas;
    private TextView textProximaRevisaoKm;
    private TextView textProximaRevisaoData;
    private TextView textNotas;
    private Button btnEditar;
    private Button btnDeletar;

    private FirebaseManager firebaseManager;
    private Manutencao manutencaoAtual;
    private String manutencaoId;
    private String veiculoId;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_manutencao);

        inicializarViews();
        obterDadosIntent();
        carregarManutencao();
    }

    private void inicializarViews() {
        scrollView = findViewById(R.id.scroll_view);
        textData = findViewById(R.id.text_data);
        textKm = findViewById(R.id.text_km);
        textTipo = findViewById(R.id.text_tipo);
        textCusto = findViewById(R.id.text_custo);
        textMecanico = findViewById(R.id.text_mecanico);
        textDescricao = findViewById(R.id.text_descricao);
        textPecas = findViewById(R.id.text_pecas);
        textProximaRevisaoKm = findViewById(R.id.text_proxima_revisao_km);
        textProximaRevisaoData = findViewById(R.id.text_proxima_revisao_data);
        textNotas = findViewById(R.id.text_notas);
        btnEditar = findViewById(R.id.btn_editar);
        btnDeletar = findViewById(R.id.btn_deletar);

        firebaseManager = FirebaseManager.getInstance();

        btnEditar.setOnClickListener(v -> editarManutencao());
        btnDeletar.setOnClickListener(v -> confirmarDeletacao());
    }

    private void obterDadosIntent() {
        manutencaoId = getIntent().getStringExtra("MANUTENCAO_ID");
        veiculoId = getIntent().getStringExtra("VEICULO_ID");

        if (manutencaoId == null) {
            Toast.makeText(this, "Erro ao carregar manutenção", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void carregarManutencao() {
        firebaseManager.obterManutencao(manutencaoId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                com.google.firebase.firestore.DocumentSnapshot snapshot = task.getResult();
                if (snapshot != null && snapshot.exists()) {
                    manutencaoAtual = snapshot.toObject(Manutencao.class);
                    if (manutencaoAtual != null) {
                        manutencaoAtual.setId(snapshot.getId());
                        preencherDados();
                    }
                } else {
                    exibirErro("Manutenção não encontrada");
                }
            } else {
                Log.e(TAG, "Erro ao carregar manutenção", task.getException());
                exibirErro("Erro ao carregar dados");
            }
        });
    }

    private void preencherDados() {
        if (manutencaoAtual == null) return;

        // Data
        if (manutencaoAtual.getDataRevisao() != null) {
            textData.setText(ManutencaoFormatter.formatarDataCompleta(manutencaoAtual.getDataRevisao()));
        } else {
            textData.setText("-");
        }

        // KM
        Long km = manutencaoAtual.getKmRevisao();
        if (km != null && km > 0) {
            textKm.setText(String.format("KM: %d", km));
        } else {
            textKm.setText("KM: -");
        }

        // Tipo
        String tipo = manutencaoAtual.getTipo();
        if (tipo != null && !tipo.isEmpty()) {
            textTipo.setText(tipo);
        } else {
            textTipo.setText("Revisão");
        }

        // Custo
        Double custo = manutencaoAtual.getCusto();
        if (custo != null && custo > 0) {
            textCusto.setText(ManutencaoFormatter.formatarCusto(custo));
        } else {
            textCusto.setText("R$ 0,00");
        }

        // Mecânico
        String mecanico = manutencaoAtual.getMecanico();
        if (mecanico != null && !mecanico.isEmpty()) {
            textMecanico.setText(mecanico);
        } else {
            textMecanico.setText("-");
        }

        // Descrição
        String descricao = manutencaoAtual.getDescricao();
        if (descricao != null && !descricao.isEmpty()) {
            textDescricao.setText(descricao);
        } else {
            textDescricao.setText("-");
        }

        // Peças
        String pecas = ManutencaoFormatter.formatarPecas(manutencaoAtual.getPecasTrocadas());
        textPecas.setText("Peças: " + pecas);

        // Próxima Revisão KM
        Long proximaKm = manutencaoAtual.getProximaRevisaoKm();
        if (proximaKm != null && proximaKm > 0) {
            textProximaRevisaoKm.setText(String.format("Próxima revisão: %s", ManutencaoFormatter.formatarKm(proximaKm)));
        } else {
            textProximaRevisaoKm.setText("Próxima revisão: -");
        }

        // Próxima Revisão Data
        Long proximaData = manutencaoAtual.getProximaRevisaoData();
        if (proximaData != null && proximaData > 0) {
            textProximaRevisaoData.setText(String.format("Data: %s",
                    ManutencaoFormatter.formatarDataCompleta(proximaData)));
        } else {
            textProximaRevisaoData.setText("Data: -");
        }

        // Notas
        String notas = manutencaoAtual.getNotas();
        if (notas != null && !notas.isEmpty()) {
            textNotas.setText(notas);
        } else {
            textNotas.setText("-");
        }
    }

    private void editarManutencao() {
        if (manutencaoAtual == null) return;

        Toast.makeText(this, "Funcionalidade de edição em desenvolvimento", Toast.LENGTH_SHORT).show();
        // Implementação futura
    }

    private void confirmarDeletacao() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar exclusão")
                .setMessage("Deseja realmente deletar esta manutenção?")
                .setPositiveButton("Sim", (dialog, which) -> deletarManutencao())
                .setNegativeButton("Não", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deletarManutencao() {
        if (manutencaoId == null) return;

        btnDeletar.setEnabled(false);

        firebaseManager.deletarManutencao(manutencaoId).addOnCompleteListener(task -> {
            btnDeletar.setEnabled(true);

            if (task.isSuccessful()) {
                Toast.makeText(DetalheManutencaoActivity.this, "Manutenção deletada com sucesso", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Log.e(TAG, "Erro ao deletar manutenção", task.getException());
                Toast.makeText(DetalheManutencaoActivity.this, "Erro ao deletar manutenção", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exibirErro(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
        finish();
    }
}