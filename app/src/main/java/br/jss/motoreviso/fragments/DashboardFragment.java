package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private MaterialCardView cardVeiculo, cardRevision, cardStats, cardTrajeto;
    private View btnNotificacao;

    private FirebaseManager firebaseManager;
    private TrajetoAdapter trajetoAdapter;

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

        cardVeiculo.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DetalheVeiculoActivity.class);
            intent.putExtra("veiculo_id", veiculo.getId());
            startActivity(intent);
        });
    }

    private void updateRevisionCard(Veiculo veiculo) {
        Long kmRestante = veiculo.getKmParaProximaRevisao();
        Long kmTotal = veiculo.getIntervaloRevisao() != null ? veiculo.getIntervaloRevisao() : 5000L;

        float progress = (kmTotal - kmRestante) / (float) kmTotal * 100;
        progressRevision.setProgress((int) Math.max(0, progress));

        textProxRevisao.setText(formatKm(kmRestante) + " km restantes");

        cardRevision.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RastreamentoActivity.class);
            startActivity(intent);
        });
    }

    private void loadStats(String veiculoId) {
        // Aqui você pode implementar lógica para carregar stats do Firebase
        // Por enquanto, usando valores de exemplo
        textKmMes.setText("320 km");
        textCustoMes.setText("R$ 150,00");
        textConsumo.setText("22.5 km/l");
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
        textUltimoTrajeto.setText(trajeto.getNome());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        textUltimoData.setText(sdf.format(new Date(trajeto.getDataCadastro())));

        textDist.setText(String.format("%.1f km", trajeto.getDistancia()));
        textVelMax.setText(String.format("%d km/h", trajeto.getVelocidadeMaxima()));

        cardTrajeto.setOnClickListener(v -> {
            // Navigate to maps or trajectory detail
        });
    }

    private String getHora() {
        int hora = new java.util.Calendar.Builder()
                .setInstant(System.currentTimeMillis())
                .build()
                .get(java.util.Calendar.HOUR_OF_DAY);

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
