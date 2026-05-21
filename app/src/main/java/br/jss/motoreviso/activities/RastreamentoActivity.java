package br.jss.motoreviso.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.managers.LocationManager;
import br.jss.motoreviso.models.Trajeto;

public class RastreamentoActivity extends AppCompatActivity implements LocationManager.OnLocationUpdateListener {
    private String veiculoId;
    private Long kmAtual;
    private TextView textTempo, textKmRodados, textVelocidadeAtual, textVelocidadeMaxima;
    private Button btnIniciar, btnParar;
    private LocationManager locationManager;
    private FirebaseManager firebaseManager;
    private Handler handler;
    private Runnable runnable;
    private long tempoDecorrido = 0;
    private boolean rastreando = false;
    private static final int PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rastreamento);

        veiculoId = getIntent().getStringExtra("VEICULO_ID");
        kmAtual = getIntent().getLongExtra("KM_ATUAL", 0);

        locationManager = LocationManager.getInstance(this);
        firebaseManager = FirebaseManager.getInstance();
        handler = new Handler(Looper.getMainLooper());

        inicializarViews();
        verificarPermissoes();

        btnIniciar.setOnClickListener(v -> iniciarRastreamento());
        btnParar.setOnClickListener(v -> pararRastreamento());
    }

    private void inicializarViews() {
        textTempo = findViewById(R.id.text_tempo);
        textKmRodados = findViewById(R.id.text_km_rodados);
        textVelocidadeAtual = findViewById(R.id.text_velocidade_atual);
        textVelocidadeMaxima = findViewById(R.id.text_velocidade_maxima);
        btnIniciar = findViewById(R.id.btn_iniciar);
        btnParar = findViewById(R.id.btn_parar);

        btnParar.setEnabled(false);
    }

    private void verificarPermissoes() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissão concedida", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void iniciarRastreamento() {
        rastreando = true;
        tempoDecorrido = 0;
        btnIniciar.setEnabled(false);
        btnParar.setEnabled(true);

        locationManager.iniciarRastreamento(kmAtual, this);
        iniciarContadorTempo();

        Toast.makeText(this, "Rastreamento iniciado", Toast.LENGTH_SHORT).show();
    }

    private void iniciarContadorTempo() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (rastreando) {
                    tempoDecorrido += 1000;
                    long segundos = tempoDecorrido / 1000;
                    long minutos = segundos / 60;
                    long segundosRestantes = segundos % 60;

                    textTempo.setText(String.format("%02d:%02d", minutos, segundosRestantes));
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(runnable);
    }

    private void pararRastreamento() {
        rastreando = false;
        handler.removeCallbacks(runnable);
        btnIniciar.setEnabled(true);
        btnParar.setEnabled(false);

        Trajeto trajeto = locationManager.pararRastreamento(kmAtual);
        if (trajeto != null) {
            trajeto.setVeiculoId(veiculoId);
            salvarTrajeto(trajeto);
        }

        Toast.makeText(this, "Rastreamento finalizado", Toast.LENGTH_SHORT).show();
    }

    private void salvarTrajeto(Trajeto trajeto) {
        firebaseManager.adicionarTrajeto(trajeto)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Trajeto salvo com sucesso!", Toast.LENGTH_SHORT).show();
                    resetarTela();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao salvar trajeto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resetarTela() {
        textTempo.setText("00:00");
        textKmRodados.setText("0 km");
        textVelocidadeAtual.setText("0 km/h");
        textVelocidadeMaxima.setText("0 km/h");
        tempoDecorrido = 0;
    }

    @Override
    public void onLocationUpdate(Double latitude, Double longitude, Double velocidadeAtual, Double velocidadeMaxima) {
        textVelocidadeAtual.setText(String.format("%.1f km/h", velocidadeAtual));
        textVelocidadeMaxima.setText(String.format("%.1f km/h", velocidadeMaxima));

        if (locationManager.getTrajetoAtivo() != null && locationManager.getTrajetoAtivo().getKmInicial() != null) {
            Double kmRodados = (double) (locationManager.getTrajetoAtivo().getKmInicial() - kmAtual);
            textKmRodados.setText(String.format("%.2f km", Math.abs(kmRodados)));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rastreando) {
            pararRastreamento();
        }
        handler.removeCallbacks(runnable);
    }
}