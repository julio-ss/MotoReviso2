package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.DetalheVeiculoActivity;
import br.jss.motoreviso.activities.MainActivity;
import br.jss.motoreviso.activities.MapTrajetoActivity;
import br.jss.motoreviso.activities.RastreamentoActivity;
import br.jss.motoreviso.adapters.TrajetoAdapter;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Trajeto;
import br.jss.motoreviso.models.Veiculo;

public class DashboardFragment extends Fragment {
    private static final String TAG = "DashboardFragment";

    private ImageView imgVeiculo;
    private TextView textGreeting, textVeiculoNome, textKmAtual, textProxRevisao;
    private TextView textCustoMes, textKmMes, textConsumo;
    private TextView textUltimoTrajeto, textUltimoData, textDist, textVelMax;
    private ProgressBar progressRevision, progressLoading;
    private ProgressBar progressOleo, progressPneus, progressFreios, progressCorrente;
    private MaterialCardView cardVeiculo, cardRevision, cardTrajeto;
    private ViewGroup cardStats;  // It's a LinearLayout in XML
    private View btnNotificacao;

    private FirebaseManager firebaseManager;
    private TrajetoAdapter trajetoAdapter;
    private Trajeto ultimoTrajeto;  // Store last trip for navigation

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        initializeViews(view);
        setupData();

        return view;
    }

    private void initializeViews(View view) {
        // Headers
        textGreeting = view.findViewById(R.id.text_greeting);
        btnNotificacao = view.findViewById(R.id.btn_notificacao);

        // Vehicle card
        imgVeiculo = view.findViewById(R.id.img_veiculo);
        textVeiculoNome = view.findViewById(R.id.text_veiculo_nome);
        textKmAtual = view.findViewById(R.id.text_km_atual);
        cardVeiculo = view.findViewById(R.id.card_veiculo);

        // Revision card
        textProxRevisao = view.findViewById(R.id.text_prox_revisao);
        progressRevision = view.findViewById(R.id.progress_revisao);
        cardRevision = view.findViewById(R.id.card_revisao);

        // Health indicators
        progressOleo = view.findViewById(R.id.progress_oleo);
        progressPneus = view.findViewById(R.id.progress_pneus);
        progressFreios = view.findViewById(R.id.progress_freios);
        progressCorrente = view.findViewById(R.id.progress_corrente);

        // Stats
        textKmMes = view.findViewById(R.id.text_km_mes);
        textCustoMes = view.findViewById(R.id.text_custo_mes);
        textConsumo = view.findViewById(R.id.text_consumo);
        cardStats = view.findViewById(R.id.card_stats);

        // Last trip
        textUltimoTrajeto = view.findViewById(R.id.text_ultimo_trajeto);
        textUltimoData = view.findViewById(R.id.text_ultimo_data);
        textDist = view.findViewById(R.id.text_dist);
        textVelMax = view.findViewById(R.id.text_vel_max);
        cardTrajeto = view.findViewById(R.id.card_trajeto);

        progressLoading = view.findViewById(R.id.progress_loading);

        firebaseManager = FirebaseManager.getInstance();

        // Set greeting
        String hora = getHora();
        textGreeting.setText(hora);
    }

    private void setupData() {
        progressLoading.setVisibility(View.VISIBLE);

        firebaseManager.carregarVeiculoPrincipal(new FirebaseManager.VeiculoCallback() {
            @Override
            public void onSuccess(Veiculo veiculo) {
                if (veiculo != null) {
                    updateVeiculoCard(veiculo);
                    updateRevisionCard(veiculo);
                    loadStats(veiculo.getId());
                    loadLastTrip(veiculo.getId());
                }
                progressLoading.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                progressLoading.setVisibility(View.GONE);
            }
        });
    }

    private void updateVeiculoCard(Veiculo veiculo) {
        textVeiculoNome.setText(veiculo.getMarca() + " " + veiculo.getModelo());
        textKmAtual.setText(formatKm(veiculo.getKmAtual()) + " km");

        if (veiculo.getUrlImagemPrincipal() != null && !veiculo.getUrlImagemPrincipal().isEmpty()) {
            Glide.with(this)
                    .load(veiculo.getUrlImagemPrincipal())
                    .centerCrop()
                    .into(imgVeiculo);
        }

        // Update health indicators
        updateHealthIndicators(veiculo);

        // Navigate to vehicle details when clicking the card or image
        View.OnClickListener openDetailsListener = v -> {
            Intent intent = new Intent(getActivity(), DetalheVeiculoActivity.class);
            intent.putExtra("veiculo_id", veiculo.getId());
            startActivity(intent);
        };

        cardVeiculo.setOnClickListener(openDetailsListener);
        imgVeiculo.setOnClickListener(openDetailsListener);
    }

    private void updateHealthIndicators(Veiculo veiculo) {
        Long healthOleo = veiculo.getHealthOleo();
        Long healthPneus = veiculo.getHealthPneus();
        Long healthFreios = veiculo.getHealthFreios();
        Long healthCorrente = veiculo.getHealthCorrente();

        if (progressOleo != null) {
            progressOleo.setProgress(Math.toIntExact(healthOleo));
        }
        if (progressPneus != null) {
            progressPneus.setProgress(Math.toIntExact(healthPneus));
        }
        if (progressFreios != null) {
            progressFreios.setProgress(Math.toIntExact(healthFreios));
        }
        if (progressCorrente != null) {
            progressCorrente.setProgress(Math.toIntExact(healthCorrente));
        }
    }

    private void updateRevisionCard(Veiculo veiculo) {
        Long kmRestante = veiculo.getKmParaProximaRevisao();
        Long kmTotal = veiculo.getIntervaloRevisao() != null ? veiculo.getIntervaloRevisao() : 5000L;

        float progress = (kmTotal - kmRestante) / (float) kmTotal * 100;
        progressRevision.setProgress((int) Math.max(0, progress));

        textProxRevisao.setText(formatKm(kmRestante) + " km restantes");

        // Navigate to Manutenções when clicking on revision card
        cardRevision.setOnClickListener(v -> {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.carregarFragment(new ManutencoesFragment());
            }
        });
    }

    private void loadStats(String veiculoId) {
        // TODO: Implement real stats loading from Firebase
        // Load actual stats for:
        // - KM driven this month (sum of all trajetos.kmRodados where month = current)
        // - Average consumption (kmRodados / fuel used)
        // - Total cost this month (sum of all maintenance costs for current month)

        // For now, hide stats if not available
        textKmMes.setText("--");
        textCustoMes.setText("--");
        textConsumo.setText("--");
    }

    private void loadLastTrip(String veiculoId) {
        firebaseManager.carregarTrajetos(new FirebaseManager.TrajetosCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> trajetos) {
                if (trajetos != null && !trajetos.isEmpty()) {
                    Trajeto ultimo = trajetos.get(0).toObject(Trajeto.class);
                    if (ultimo != null) {
                        updateTrajetoCard(ultimo);
                    }
                }
            }

            @Override
            public void onError(String error) {
                // Silent error, just hide the card
            }
        });
    }

    private void updateTrajetoCard(Trajeto trajeto) {
        this.ultimoTrajeto = trajeto;  // Store for navigation

        // Use origem and destino instead of nome
        String trajName = (trajeto.getOrigem() != null ? trajeto.getOrigem() : "Trajeto") +
                         " → " +
                         (trajeto.getDestino() != null ? trajeto.getDestino() : "");
        textUltimoTrajeto.setText(trajName);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        textUltimoData.setText(sdf.format(new Date(trajeto.getDataCadastro())));

        // Use getKmRodados() instead of getDistancia()
        Double kmRodados = trajeto.getKmRodados() != null ? trajeto.getKmRodados() : 0.0;
        textDist.setText(String.format("%.1f km", kmRodados));

        Integer velMax = trajeto.getVelocidadeMaxima() != null ? trajeto.getVelocidadeMaxima().intValue() : 0;
        textVelMax.setText(String.format("%d km/h", velMax));

        // Navigate to map trajectory when clicked
        cardTrajeto.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MapTrajetoActivity.class);
            intent.putExtra("trajeto_id", ultimoTrajeto.getId());
            startActivity(intent);
        });
    }

    private String getHora() {
        // Use Calendar.getInstance() for API 24 compatibility instead of Calendar.Builder (requires API 26)
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hora = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        if (hora >= 5 && hora < 12) {
            return "Bom dia";
        } else if (hora >= 12 && hora < 18) {
            return "Boa tarde";
        } else {
            return "Boa noite";
        }
    }

    private String formatKm(Long km) {
        if (km == null) return "0";
        return String.format(Locale.getDefault(), "%,d", km).replace(",", ".");
    }

    @Override
    public void onResume() {
        super.onResume();
        setupData();
    }
}
