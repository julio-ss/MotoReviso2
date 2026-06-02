package br.jss.motoreviso.fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.MapTrajetoActivity;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Trajeto;
import br.jss.motoreviso.services.RastreamentoService;

public class RastreamentoEmTempoRealFragment extends Fragment {
    private static final String TAG = "RastreamentoEmTempoReal";
    private static final int PERMISSION_CODE = 100;

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

    // Rastreamento com Serviço Real
    private RastreamentoService rastreamentoService;
    private boolean servicoBound = false;
    private String veiculoId;
    private Long kmInicial;

    // Timer para atualizar cronômetro
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    // Receiver para broadcast de rastreamento
    private BroadcastReceiver trackingReceiver;

    // Firebase
    private FirebaseManager firebaseManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rastreamento_tempo_real, container, false);

        firebaseManager = FirebaseManager.getInstance();

        // Inicializar receiver de rastreamento
        if (trackingReceiver == null) {
            trackingReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    double vel = intent.getDoubleExtra(RastreamentoService.EXTRA_VELOCIDADE, 0);
                    double velMax = intent.getDoubleExtra(RastreamentoService.EXTRA_VELOCIDADE_MAX, 0);
                    double dist = intent.getDoubleExtra(RastreamentoService.EXTRA_DISTANCIA, 0);
                    long tempo = intent.getLongExtra(RastreamentoService.EXTRA_TEMPO, 0);

                    textVelocidade.setText(String.format(Locale.getDefault(), "%.0f", vel));
                    textVelMax.setText(String.format(Locale.getDefault(), "%.1f", velMax));
                    textDistancia.setText(String.format(Locale.getDefault(), "%.1f", dist));
                    textTempo.setText(formatarTempo(tempo));

                    int progress = (int) Math.min(vel, 200);
                    progressSpeedometer.setProgress(progress);

                    textAltitude.setText(String.valueOf(600 + (int)(Math.random() * 100)));
                    textInclinacao.setText(String.valueOf((int)(Math.random() * 15)));
                }
            };
        }

        inicializarViews(view);
        configurarListeners();
        verificarPermissoes();
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
        // Verificar permissão de localização
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            verificarPermissoes();
            return;
        }

        // Garantir que qualquer instância anterior foi parada
        if (servicoBound) {
            getContext().unbindService(serviceConnection);
            servicoBound = false;
        }

        Intent intent = new Intent(getContext(), RastreamentoService.class);
        intent.putExtra("VEICULO_ID", veiculoId != null ? veiculoId : "veiculo_demo");
        intent.putExtra("KM_INICIAL", kmInicial != null ? kmInicial : 0L);
        ContextCompat.startForegroundService(getContext(), intent);

        getContext().bindService(new Intent(getContext(), RastreamentoService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);

        // Iniciar timer para atualizar cronômetro
        iniciarTimer();

        // Atualizar estados dos botões
        btnIniciar.setEnabled(false);
        btnPausar.setEnabled(true);
        btnParar.setEnabled(true);

        Toast.makeText(getContext(), "Rastreamento iniciado", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Rastreamento iniciado com serviço real");
    }

    private void pausarRastreamento() {
        if (!servicoBound || rastreamentoService == null) return;

        Intent intent = new Intent(getContext(), RastreamentoService.class);
        if (rastreamentoService.isPausado()) {
            intent.setAction(RastreamentoService.ACTION_RESUME);
            btnPausar.setText("Pausar");
            timerHandler.post(timerRunnable);
            Toast.makeText(getContext(), "Rastreamento retomado", Toast.LENGTH_SHORT).show();
        } else {
            intent.setAction(RastreamentoService.ACTION_PAUSE);
            btnPausar.setText("Retomar");
            timerHandler.removeCallbacks(timerRunnable);
            Toast.makeText(getContext(), "Rastreamento pausado", Toast.LENGTH_SHORT).show();
        }
        getContext().startService(intent);
    }

    private void pararRastreamento() {
        if (!servicoBound || rastreamentoService == null) return;

        timerHandler.removeCallbacks(timerRunnable);

        // Obter dados finais do trajeto
        Trajeto trajeto = rastreamentoService.finalizarETrajeto();
        Log.d(TAG, String.format("Encerrando: %.3f km, %d pontos",
                trajeto.getKmRodados() != null ? trajeto.getKmRodados() : 0.0,
                trajeto.getPontos() != null ? trajeto.getPontos().size() : 0));

        // Resetar UI imediatamente
        resetarTela();
        atualizarBotoes(false);

        // Parar o serviço
        Intent stopIntent = new Intent(getContext(), RastreamentoService.class);
        stopIntent.setAction(RastreamentoService.ACTION_STOP);
        getContext().startService(stopIntent);

        if (servicoBound) {
            getContext().unbindService(serviceConnection);
            servicoBound = false;
        }
        rastreamentoService = null;

        // Salvar trajeto se tem pontos
        boolean temPontos = trajeto.getPontos() != null && !trajeto.getPontos().isEmpty();
        if (veiculoId != null && temPontos) {
            salvarTrajeto(trajeto);
        } else if (!temPontos) {
            Toast.makeText(getContext(), "Nenhum ponto GPS registrado. Trajeto não salvo.", Toast.LENGTH_LONG).show();
        }
    }

    private void iniciarTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (servicoBound && rastreamentoService != null
                        && rastreamentoService.isRastreando()
                        && !rastreamentoService.isPausado()) {
                    long ms = rastreamentoService.getTempoDecorrido();
                    textTempo.setText(formatarTempo(ms));
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
    }

    private void salvarTrajeto(Trajeto trajeto) {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            trajeto.setUserId(user.getUid());
        }

        firebaseManager.adicionarTrajeto(trajeto)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Trajeto salvo: " + ref.getId());
                    Toast.makeText(getContext(), "Trajeto salvo com sucesso!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao salvar trajeto", e);
                    Toast.makeText(getContext(), "Erro ao salvar trajeto: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void resetarTela() {
        textTempo.setText("00:00");
        textDistancia.setText("0.0");
        textVelocidade.setText("0");
        textVelMax.setText("0");
        textAltitude.setText("0");
        textInclinacao.setText("0");
        progressSpeedometer.setProgress(0);
    }

    private void atualizarBotoes(boolean rastreando) {
        btnIniciar.setEnabled(!rastreando);
        btnPausar.setEnabled(rastreando);
        btnParar.setEnabled(rastreando);
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
        timerHandler.postDelayed(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            textHora.setText(sdf.format(new Date()));
            atualizarHora(); // Repetir a cada minuto
        }, 60000);
    }

    private void verificarPermissoes() {
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_CODE + 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permissão de localização necessária para rastreamento.",
                        Toast.LENGTH_LONG).show();
                btnIniciar.setEnabled(false);
            }
        }
    }

    private String formatarTempo(long ms) {
        long seg = TimeUnit.MILLISECONDS.toSeconds(ms);
        long horas = seg / 3600;
        long min = (seg % 3600) / 60;
        long s = seg % 60;
        if (horas > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", horas, min, s);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", min, s);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            RastreamentoService.LocalBinder lb = (RastreamentoService.LocalBinder) binder;
            rastreamentoService = lb.getService();
            servicoBound = true;

            sincronizarUiComServico();
            if (timerRunnable != null) {
                timerHandler.post(timerRunnable);
            }
            Log.d(TAG, "Serviço de rastreamento conectado");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicoBound = false;
            rastreamentoService = null;
            timerHandler.removeCallbacks(timerRunnable);
            Log.d(TAG, "Serviço de rastreamento desconectado");
        }
    };

    private void sincronizarUiComServico() {
        if (rastreamentoService == null) return;
        double dist = rastreamentoService.getDistanciaTotal();
        double vel = rastreamentoService.getVelocidadeAtual();
        double velMax = rastreamentoService.getVelocidadeMaxima();
        long tempo = rastreamentoService.getTempoDecorrido();

        textDistancia.setText(String.format(Locale.getDefault(), "%.1f", dist));
        textVelocidade.setText(String.format(Locale.getDefault(), "%.0f", vel));
        textVelMax.setText(String.format(Locale.getDefault(), "%.1f", velMax));
        textTempo.setText(formatarTempo(tempo));

        int progress = (int) Math.min(vel, 200);
        progressSpeedometer.setProgress(progress);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (trackingReceiver != null) {
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                    trackingReceiver, new IntentFilter(RastreamentoService.BROADCAST_UPDATE));
        }
    }

    @Override
    public void onPause() {
        if (trackingReceiver != null) {
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(trackingReceiver);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        if (servicoBound) {
            getContext().unbindService(serviceConnection);
            servicoBound = false;
        }
    }
}
