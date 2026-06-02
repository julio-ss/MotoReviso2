package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.DatePickerDialog;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private TextView textProgressPercent, textDataProximaRevisao;
    private MaterialButton btnAgendarRevisao;
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
        textProgressPercent = view.findViewById(R.id.text_progress_percent);
        textDataProximaRevisao = view.findViewById(R.id.text_data_proxima_revisao);
        btnAgendarRevisao = view.findViewById(R.id.btn_agendar_revisao);
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

        // Set greeting with time of day and pilot name
        String greeting = getGreetingWithPilot();
        textGreeting.setText(greeting);
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
        // Get health values, use defaults if not set
        Long healthOleo = veiculo.getHealthOleo() > 0 ? veiculo.getHealthOleo() : 72L;
        Long healthPneus = veiculo.getHealthPneus() > 0 ? veiculo.getHealthPneus() : 59L;
        Long healthFreios = veiculo.getHealthFreios() > 0 ? veiculo.getHealthFreios() : 89L;
        Long healthCorrente = veiculo.getHealthCorrente() > 0 ? veiculo.getHealthCorrente() : 41L;

        // Update progress bars and text values
        updateHealthIndicator(progressOleo, healthOleo);
        updateHealthIndicator(progressPneus, healthPneus);
        updateHealthIndicator(progressFreios, healthFreios);
        updateHealthIndicator(progressCorrente, healthCorrente);
    }

    private void updateHealthIndicator(ProgressBar progressBar, Long value) {
        if (progressBar != null) {
            int progressValue = Math.toIntExact(value);
            progressBar.setProgress(progressValue);
        }
    }

    private void updateRevisionCard(Veiculo veiculo) {
        Long kmRestante = veiculo.getKmParaProximaRevisao();
        Long kmTotal = veiculo.getIntervaloRevisao() != null ? veiculo.getIntervaloRevisao() : 5000L;

        // Calculate progress percentage
        float progress = (kmTotal - kmRestante) / (float) kmTotal * 100;
        int progressPercent = (int) Math.max(0, Math.min(100, progress));

        // Update progress bar and percentage text
        progressRevision.setProgress(progressPercent);
        if (textProgressPercent != null) {
            textProgressPercent.setText(String.valueOf(progressPercent));
        }

        // Show remaining KM below
        textProxRevisao.setText(formatKm(kmRestante) + " km restantes");

        // Show scheduled revision date if available
        if (veiculo.getDataProximaRevisao() != null && veiculo.getDataProximaRevisao() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dataFormatada = sdf.format(new Date(veiculo.getDataProximaRevisao()));
            textDataProximaRevisao.setText("Agendada: " + dataFormatada);
        } else {
            textDataProximaRevisao.setText("Data a agendar");
        }

        // Schedule revision date when clicking Agendar button
        btnAgendarRevisao.setOnClickListener(v -> {
            showDatePickerForRevision(veiculo);
        });

        // Navigate to Manutenções when clicking on revision card
        cardRevision.setOnClickListener(v -> {
            if (getActivity() != null && getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.carregarFragment(new ManutencoesFragment());
            }
        });
    }

    private void showDatePickerForRevision(Veiculo veiculo) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        // If there's a scheduled date, use it as initial date
        if (veiculo.getDataProximaRevisao() != null && veiculo.getDataProximaRevisao() > 0) {
            calendar.setTimeInMillis(veiculo.getDataProximaRevisao());
        } else {
            // Default: next week
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 7);
        }

        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
            getContext(),
            (view, selectedYear, selectedMonth, selectedDay) -> {
                // Create calendar with selected date
                java.util.Calendar selectedCalendar = java.util.Calendar.getInstance();
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                long selectedDateMillis = selectedCalendar.getTimeInMillis();

                // Update vehicle with scheduled date
                veiculo.setDataProximaRevisao(selectedDateMillis);

                // Save to Firebase
                firebaseManager.atualizarVeiculo(veiculo.getId(), veiculo)
                    .addOnSuccessListener(unused -> {
                        // Update display with new date
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String dataFormatada = sdf.format(new Date(selectedDateMillis));
                        textDataProximaRevisao.setText("Agendada: " + dataFormatada);

                        // Show confirmation
                        Log.d(TAG, "Revisão agendada para: " + dataFormatada);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Erro ao agendar revisão: " + e.getMessage());
                    });
            },
            year, month, day
        );

        datePickerDialog.show();
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
                    DocumentSnapshot doc = trajetos.get(0);
                    Trajeto ultimo = doc.toObject(Trajeto.class);
                    if (ultimo != null) {
                        // Ensure we have the document ID (important for navigation)
                        if (ultimo.getId() == null || ultimo.getId().isEmpty()) {
                            ultimo.setId(doc.getId());
                        }
                        updateTrajetoCard(ultimo);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Erro ao carregar trajetos: " + error);
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
            intent.putExtra("TRAJETO_ID", ultimoTrajeto.getId());
            startActivity(intent);
        });
    }

    private String getGreetingWithPilot() {
        // Get time of day
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int hora = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        String periodGreeting;
        if (hora >= 5 && hora < 12) {
            periodGreeting = "Bom dia";
        } else if (hora >= 12 && hora < 18) {
            periodGreeting = "Boa tarde";
        } else {
            periodGreeting = "Boa noite";
        }

        // Get pilot name from Firebase Auth
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String pilotName = "Piloto";
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                pilotName = user.getDisplayName().split(" ")[0];  // Get first name only
            } else if (user.getEmail() != null) {
                pilotName = user.getEmail().split("@")[0];  // Use email prefix as fallback
            }
        }

        return periodGreeting + ", " + pilotName;
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
