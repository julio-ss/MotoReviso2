package br.jss.motoreviso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import br.jss.motoreviso.R;
import br.jss.motoreviso.adapters.DetalheVeiculoTabAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Veiculo;
import br.jss.motoreviso.utils.ImagemLoader;
import br.jss.motoreviso.utils.SystemBarHelper;

public class DetalheVeiculoActivity extends AppCompatActivity {
    private String veiculoId;
    private ImageView imgVeiculo;
    private TextView textMarcaModelo, textPlaca, textKmAtual, textAlerta;
    private Button btnEditar;
    private FloatingActionButton fabRastrear;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;
    private Veiculo veiculoAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalhe_veiculo);
        overridePendingTransition(R.anim.anim_fade_in, R.anim.anim_fade_out);

        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        veiculoId = getIntent().getStringExtra("VEICULO_ID");
        if (veiculoId == null) {
            finish();
            return;
        }

        firebaseManager = FirebaseManager.getInstance();
        inicializarViews();
        carregarVeiculo();

        btnEditar.setOnClickListener(v -> abrirEditarVeiculo());
        fabRastrear.setOnClickListener(v -> abrirRastreamento());
    }

    private void inicializarViews() {
        imgVeiculo = findViewById(R.id.img_veiculo_detalhe);
        textMarcaModelo = findViewById(R.id.text_marca_modelo);
        textPlaca = findViewById(R.id.text_placa);
        textKmAtual = findViewById(R.id.text_km_atual);
        textAlerta = findViewById(R.id.text_alerta);
        btnEditar = findViewById(R.id.btn_editar);
        fabRastrear = findViewById(R.id.fab_rastrear);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void carregarVeiculo() {
        progressBar.setVisibility(android.view.View.VISIBLE);

        firebaseManager.obterVeiculo(veiculoId).addOnCompleteListener(task -> {
            progressBar.setVisibility(android.view.View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                veiculoAtual = task.getResult().toObject(Veiculo.class);
                if (veiculoAtual != null) {
                    veiculoAtual.setId(veiculoId);
                    exibirDadosVeiculo();
                    setupViewPager();
                }
            } else {
                Toast.makeText(this, "Erro ao carregar veículo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exibirDadosVeiculo() {
        ImagemLoader.carregarImagem(this, imgVeiculo, veiculoAtual.getUrlImagemPrincipal());

        textMarcaModelo.setText(veiculoAtual.getMarca() + " " + veiculoAtual.getModelo());
        textPlaca.setText("Placa: " + veiculoAtual.getPlaca());
        textKmAtual.setText("KM: " + (veiculoAtual.getKmAtual() != null ? veiculoAtual.getKmAtual() : 0));

        if (veiculoAtual.precisaRevisao() != null && veiculoAtual.precisaRevisao()) {
            textAlerta.setVisibility(android.view.View.VISIBLE);
            textAlerta.setText("⚠️ REVISÃO URGENTE!");
            textAlerta.setTextColor(getColor(R.color.error));
        } else {
            textAlerta.setVisibility(android.view.View.GONE);
        }
    }

    private void setupViewPager() {
        DetalheVeiculoTabAdapter adapter = new DetalheVeiculoTabAdapter(this, veiculoId, veiculoAtual.getTipo());
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Especificações");
                    break;
                case 1:
                    tab.setText("Manutenções");
                    break;
                case 2:
                    tab.setText("Trajetos");
                    break;
                case 3:
                    tab.setText("Imagens");
                    break;
            }
        }).attach();
    }

    private void abrirEditarVeiculo() {
        Intent intent = new Intent(this, EditarVeiculoActivity.class);
        intent.putExtra("VEICULO_ID", veiculoId);
        startActivity(intent);
        overridePendingTransition(R.anim.anim_slide_in, R.anim.anim_slide_out);
    }

    private void abrirRastreamento() {
        Intent intent = new Intent(this, RastreamentoActivity.class);
        intent.putExtra("VEICULO_ID", veiculoId);
        intent.putExtra("KM_ATUAL", veiculoAtual.getKmAtual());
        startActivity(intent);
        overridePendingTransition(R.anim.anim_slide_in, R.anim.anim_slide_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarVeiculo();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.anim_fade_out, R.anim.anim_fade_in);
    }
}