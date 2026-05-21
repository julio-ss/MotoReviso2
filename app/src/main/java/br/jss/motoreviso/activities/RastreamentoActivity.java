package br.jss.motoreviso.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Trajeto;
import br.jss.motoreviso.services.RastreamentoService;

public class RastreamentoActivity extends AppCompatActivity {
    private static final String TAG = "RastreamentoActivity";
    private static final int PERMISSION_CODE = 100;

    private String veiculoId;
    private Long kmAtual;
    private TextView textTempo, textKmRodados, textVelocidadeAtual, textVelocidadeMaxima;
    private Button btnIniciar, btnPausar, btnParar;
    private FirebaseManager firebaseManager;

    private RastreamentoService rastreamentoService;
    private boolean servicoBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            RastreamentoService.LocalBinder lb = (RastreamentoService.LocalBinder) binder;
            rastreamentoService = lb.getService();
            servicoBound = true;
            atualizarBotoes(true, rastreamentoService.isPausado());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servicoBound = false;
            rastreamentoService = null;
        }
    };

    private final BroadcastReceiver trackingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RastreamentoService.BROADCAST_UPDATE.equals(intent.getAction())) {
                double vel = intent.getDoubleExtra(RastreamentoService.EXTRA_VELOCIDADE, 0);
                double velMax = intent.getDoubleExtra(RastreamentoService.EXTRA_VELOCIDADE_MAX, 0);
                double dist = intent.getDoubleExtra(RastreamentoService.EXTRA_DISTANCIA, 0);
                long tempo = intent.getLongExtra(RastreamentoService.EXTRA_TEMPO, 0);
                boolean pausado = intent.getBooleanExtra(RastreamentoService.EXTRA_PAUSADO, false);

                textVelocidadeAtual.setText(String.format(Locale.getDefault(), "%.1f km/h", vel));
                textVelocidadeMaxima.setText(String.format(Locale.getDefault(), "%.1f km/h", velMax));
                textKmRodados.setText(String.format(Locale.getDefault(), "%.2f km", dist));

                long seg = TimeUnit.MILLISECONDS.toSeconds(tempo);
                textTempo.setText(String.format(Locale.getDefault(), "%02d:%02d", seg / 60, seg % 60));

                atualizarBotoes(true, pausado);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rastreamento);

        veiculoId = getIntent().getStringExtra("VEICULO_ID");
        kmAtual = getIntent().getLongExtra("KM_ATUAL", 0L);

        firebaseManager = FirebaseManager.getInstance();
        inicializarViews();
        verificarPermissoes();
    }

    private void inicializarViews() {
        textTempo = findViewById(R.id.text_tempo);
        textKmRodados = findViewById(R.id.text_km_rodados);
        textVelocidadeAtual = findViewById(R.id.text_velocidade_atual);
        textVelocidadeMaxima = findViewById(R.id.text_velocidade_maxima);
        btnIniciar = findViewById(R.id.btn_iniciar);
        btnPausar = findViewById(R.id.btn_pausar);
        btnParar = findViewById(R.id.btn_parar);

        btnIniciar.setOnClickListener(v -> iniciarRastreamento());
        btnPausar.setOnClickListener(v -> pausarOuRetomarRastreamento());
        btnParar.setOnClickListener(v -> pararRastreamento());

        atualizarBotoes(false, false);
    }

    private void verificarPermissoes() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_CODE + 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissão de localização necessária para rastreamento.", Toast.LENGTH_LONG).show();
                btnIniciar.setEnabled(false);
            }
        }
    }

    private void iniciarRastreamento() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            verificarPermissoes();
            return;
        }

        Intent intent = new Intent(this, RastreamentoService.class);
        intent.putExtra("VEICULO_ID", veiculoId);
        intent.putExtra("KM_INICIAL", kmAtual);
        ContextCompat.startForegroundService(this, intent);

        bindService(new Intent(this, RastreamentoService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        Toast.makeText(this, "Rastreamento iniciado", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Rastreamento iniciado para veículo: " + veiculoId);
    }

    private void pausarOuRetomarRastreamento() {
        if (!servicoBound || rastreamentoService == null) return;

        Intent intent = new Intent(this, RastreamentoService.class);
        if (rastreamentoService.isPausado()) {
            intent.setAction(RastreamentoService.ACTION_RESUME);
        } else {
            intent.setAction(RastreamentoService.ACTION_PAUSE);
        }
        startService(intent);
    }

    private void pararRastreamento() {
        if (!servicoBound || rastreamentoService == null) return;

        Trajeto trajeto = rastreamentoService.finalizarETrajeto();
        Log.d(TAG, String.format("Parando: %.2f km, %d pontos",
                trajeto.getKmRodados() != null ? trajeto.getKmRodados() : 0.0,
                trajeto.getPontos() != null ? trajeto.getPontos().size() : 0));

        Intent stopIntent = new Intent(this, RastreamentoService.class);
        stopIntent.setAction(RastreamentoService.ACTION_STOP);
        startService(stopIntent);

        if (servicoBound) {
            unbindService(serviceConnection);
            servicoBound = false;
        }

        if (veiculoId != null && trajeto.getKmRodados() != null && trajeto.getKmRodados() > 0) {
            salvarTrajeto(trajeto);
        }

        resetarTela();
        atualizarBotoes(false, false);
    }

    private void salvarTrajeto(Trajeto trajeto) {
        firebaseManager.adicionarTrajeto(trajeto)
                .addOnSuccessListener(ref -> {
                    Log.d(TAG, "Trajeto salvo: " + ref.getId());
                    Toast.makeText(this, "Trajeto salvo com sucesso!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao salvar trajeto", e);
                    Toast.makeText(this, "Erro ao salvar trajeto.", Toast.LENGTH_SHORT).show();
                });
    }

    private void resetarTela() {
        textTempo.setText("00:00");
        textKmRodados.setText("0 km");
        textVelocidadeAtual.setText("0 km/h");
        textVelocidadeMaxima.setText("0 km/h");
    }

    private void atualizarBotoes(boolean rastreando, boolean pausado) {
        btnIniciar.setEnabled(!rastreando);
        btnPausar.setEnabled(rastreando);
        btnParar.setEnabled(rastreando);
        btnPausar.setText(pausado ? "Continuar" : "Pausar");
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(RastreamentoService.BROADCAST_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(trackingReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(trackingReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(trackingReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver não registrado", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (servicoBound) {
            unbindService(serviceConnection);
            servicoBound = false;
        }
    }
}
