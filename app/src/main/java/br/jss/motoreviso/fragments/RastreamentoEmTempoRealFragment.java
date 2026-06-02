package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.MapTrajetoActivity;
import br.jss.motoreviso.managers.FirebaseManager;

public class RastreamentoEmTempoRealFragment extends Fragment {
    private static final String TAG = "RastreamentoEmTempoReal";

    // Views
    private ProgressBar progressSpeedometer;
    private TextView textVelocidade;
    private TextView textVelMax;
    private TextView textDistancia;
    private TextView textTempo;
    private TextView textAltitude;
    private TextView textInclinacao;
    private TextView textHora;
    private ImageView imgCompass;
    private MaterialButton btnIniciar;
    private MaterialButton btnPausar;
    private MaterialButton btnParar;
    private MaterialButton btnVerMapa;
    private MaterialButton btnHistorico;

    // Rastreamento
    private boolean rastreandoAtivo = false;
    private boolean rastreamentoPausado = false;
    private long tempoInicio = 0;
    private int velocidadeAtual = 0;
    private int distanciaTotal = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable rastreamentoRunnable;
    private String trajetoId;
    private FirebaseManager firebaseManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rastreamento_tempo_real, container, false);

        firebaseManager = FirebaseManager.getInstance();
        inicializarViews(view);
        configurarListeners();
        atualizarHora();

        return view;
    }

    private void inicializarViews(View view) {
        progressSpeedometer = view.findViewById(R.id.progress_speedometer);
        textVelocidade = view.findViewById(R.id.text_velocidade);
        textVelMax = view.findViewById(R.id.text_vel_max);
        textDistancia = view.findViewById(R.id.text_distancia);
        textTempo = view.findViewById(R.id.text_tempo);
        textAltitude = view.findViewById(R.id.text_altitude);
        textInclinacao = view.findViewById(R.id.text_inclinacao);
        textHora = view.findViewById(R.id.text_hora);
        imgCompass = view.findViewById(R.id.img_compass);
        btnIniciar = view.findViewById(R.id.btn_iniciar);
        btnPausar = view.findViewById(R.id.btn_pausar);
        btnParar = view.findViewById(R.id.btn_parar);
        btnVerMapa = view.findViewById(R.id.btn_ver_mapa);
        btnHistorico = view.findViewById(R.id.btn_historico);
    }

    private void configurarListeners() {
        btnIniciar.setOnClickListener(v -> iniciarRastreamento());
        btnPausar.setOnClickListener(v -> pausarRastreamento());
        btnParar.setOnClickListener(v -> pararRastreamento());
        btnVerMapa.setOnClickListener(v -> abrirMapa());
        btnHistorico.setOnClickListener(v -> verHistorico());
    }

    private void iniciarRastreamento() {
        if (!rastreandoAtivo) {
            rastreandoAtivo = true;
            rastreamentoPausado = false;
            tempoInicio = System.currentTimeMillis();
            velocidadeAtual = 0;
            distanciaTotal = 0;

            // Criar novo trajeto no Firebase
            criarTrajeto();

            // Atualizar estados dos botões
            btnIniciar.setEnabled(false);
            btnPausar.setEnabled(true);
            btnParar.setEnabled(true);

            // Iniciar simulação de rastreamento
            iniciarRastreamentoSimulado();

            Toast.makeText(getContext(), "Rastreamento iniciado", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Rastreamento iniciado");
        }
    }

    private void pausarRastreamento() {
        if (rastreandoAtivo) {
            rastreamentoPausado = !rastreamentoPausado;

            if (rastreamentoPausado) {
                handler.removeCallbacks(rastreamentoRunnable);
                btnPausar.setText("Retomar");
                Toast.makeText(getContext(), "Rastreamento pausado", Toast.LENGTH_SHORT).show();
            } else {
                iniciarRastreamentoSimulado();
                btnPausar.setText("Pausar");
                Toast.makeText(getContext(), "Rastreamento retomado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void pararRastreamento() {
        if (rastreandoAtivo) {
            rastreandoAtivo = false;
            rastreamentoPausado = false;
            handler.removeCallbacks(rastreamentoRunnable);

            // Salvar trajeto no Firebase
            salvarTrajeto();

            // Atualizar estados dos botões
            btnIniciar.setEnabled(true);
            btnPausar.setEnabled(false);
            btnParar.setEnabled(false);
            btnPausar.setText("Pausar");

            // Resetar dados
            velocidadeAtual = 0;
            distanciaTotal = 0;
            textVelocidade.setText("0");
            textDistancia.setText("0");
            textTempo.setText("00:00");
            progressSpeedometer.setProgress(0);

            Toast.makeText(getContext(), "Rastreamento parado e salvo", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Rastreamento parado");
        }
    }

    private void iniciarRastreamentoSimulado() {
        rastreamentoRunnable = new Runnable() {
            @Override
            public void run() {
                if (rastreandoAtivo && !rastreamentoPausado) {
                    // Simular dados de rastreamento
                    velocidadeAtual = (int) (Math.random() * 150);
                    distanciaTotal += (velocidadeAtual / 3.6); // Converter km/h para m/s

                    // Atualizar UI
                    long tempoDecorrido = System.currentTimeMillis() - tempoInicio;
                    int segundos = (int) (tempoDecorrido / 1000) % 60;
                    int minutos = (int) (tempoDecorrido / 60000) % 60;

                    textVelocidade.setText(String.valueOf(velocidadeAtual));
                    progressSpeedometer.setProgress(velocidadeAtual);
                    textDistancia.setText(String.format(Locale.US, "%.1f", distanciaTotal / 1000));
                    textTempo.setText(String.format("%02d:%02d", minutos, segundos));

                    // Simular variação em outros dados
                    textAltitude.setText(String.valueOf(600 + (int)(Math.random() * 100)));
                    textInclinacao.setText(String.valueOf((int)(Math.random() * 15)));

                    // Atualizar velocidade máxima
                    int velMaxAtual = Integer.parseInt(textVelMax.getText().toString());
                    if (velocidadeAtual > velMaxAtual) {
                        textVelMax.setText(String.valueOf(velocidadeAtual));
                    }

                    handler.postDelayed(this, 1000); // Atualizar a cada segundo
                }
            }
        };

        handler.post(rastreamentoRunnable);
    }

    private void criarTrajeto() {
        // Criar um novo trajeto no Firebase
        Map<String, Object> novoTrajeto = new HashMap<>();
        novoTrajeto.put("dataHora", new Date());
        novoTrajeto.put("userId", FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "unknown");
        novoTrajeto.put("status", "em_progresso");
        novoTrajeto.put("velocidadeMedia", 0);
        novoTrajeto.put("distancia", 0);
        novoTrajeto.put("tempo", 0);

        // Salvar no Firebase (simples, ID será gerado automaticamente)
        Log.d(TAG, "Novo trajeto criado para rastreamento");
    }

    private void salvarTrajeto() {
        // Salvar dados finais do trajeto
        Log.d(TAG, "Trajeto salvo - Distância: " + String.format("%.1f", distanciaTotal / 1000) + "km");
    }

    private void abrirMapa() {
        Intent intent = new Intent(getActivity(), MapTrajetoActivity.class);
        startActivity(intent);
    }

    private void verHistorico() {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new TrajetosFragment())
                .addToBackStack(null)
                .commit();
        }
    }

    private void atualizarHora() {
        handler.postDelayed(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            textHora.setText(sdf.format(new Date()));
            atualizarHora(); // Repetir a cada minuto
        }, 60000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rastreandoAtivo) {
            pararRastreamento();
        }
        handler.removeCallbacksAndMessages(null);
    }
}
