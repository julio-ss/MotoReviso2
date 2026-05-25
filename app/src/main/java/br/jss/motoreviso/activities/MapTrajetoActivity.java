package br.jss.motoreviso.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Trajeto;

public class MapTrajetoActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MapTrajetoActivity";

    private GoogleMap googleMap;
    private TextView textDistancia, textDuracao, textVelMax, textSemDados;
    private LinearLayout layoutCarregando, layoutSemDados;
    private FirebaseManager firebaseManager;
    private String trajetoId;
    private Trajeto trajeto;

    // flags para controlar a race condition entre mapa e dados
    private boolean mapaPronto = false;
    private boolean dadosProntos = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_trajeto);

        trajetoId = getIntent().getStringExtra("TRAJETO_ID");
        firebaseManager = FirebaseManager.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        textDistancia = findViewById(R.id.text_distancia);
        textDuracao = findViewById(R.id.text_duracao);
        textVelMax = findViewById(R.id.text_vel_max);
        textSemDados = findViewById(R.id.text_sem_dados);
        layoutCarregando = findViewById(R.id.layout_carregando);
        layoutSemDados = findViewById(R.id.layout_sem_dados);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (trajetoId != null) {
            carregarTrajeto();
        } else {
            mostrarErro("ID do trajeto inválido");
        }
    }

    private void carregarTrajeto() {
        Log.d(TAG, "→ Carregando trajeto: " + trajetoId);
        layoutCarregando.setVisibility(View.VISIBLE);
        layoutSemDados.setVisibility(View.GONE);

        firebaseManager.obterTrajeto(trajetoId).addOnCompleteListener(task -> {
            if (!isFinishing() && !isDestroyed()) {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    try {
                        trajeto = task.getResult().toObject(Trajeto.class);
                        if (trajeto != null) {
                            trajeto.setId(task.getResult().getId());
                            int numPontos = trajeto.getPontos() != null ? trajeto.getPontos().size() : 0;
                            Log.d(TAG, "✓ Trajeto carregado — " + numPontos + " pontos GPS");
                            Log.d(TAG, "  Distância: " + (trajeto.getKmRodados() != null ?
                                    String.format("%.3f km", trajeto.getKmRodados()) : "null"));
                            Log.d(TAG, "  Duração: " + trajeto.getDuracao() + " min");
                            exibirEstatisticas();
                            dadosProntos = true;
                            if (mapaPronto) {
                                plotarRotaOuMostrarVazio();
                            }
                        } else {
                            Log.e(TAG, "✗ Trajeto deserializado como null");
                            mostrarErro("Erro ao ler dados do trajeto");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "✗ Erro ao desserializar trajeto", e);
                        mostrarErro("Erro ao processar trajeto: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "✗ Trajeto não encontrado — " + trajetoId, task.getException());
                    mostrarErro("Trajeto não encontrado");
                }
            }
        });
    }

    private void exibirEstatisticas() {
        if (trajeto == null) return;

        Double dist = trajeto.getKmRodados();
        textDistancia.setText(String.format(Locale.getDefault(), "%.2f km", dist != null ? dist : 0.0));

        Long duracao = trajeto.getDuracao();
        if (duracao != null && duracao > 0) {
            long horas = duracao / 60;
            long min = duracao % 60;
            if (horas > 0) {
                textDuracao.setText(String.format(Locale.getDefault(), "%dh %02dm", horas, min));
            } else {
                textDuracao.setText(String.format(Locale.getDefault(), "%d min", min));
            }
        } else {
            textDuracao.setText("< 1 min");
        }

        Double velMax = trajeto.getVelocidadeMaxima();
        textVelMax.setText(String.format(Locale.getDefault(), "%.0f km/h", velMax != null ? velMax : 0.0));
    }

    @Override
    public void onMapReady(GoogleMap map) {
        try {
            this.googleMap = map;
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.getUiSettings().setMapToolbarEnabled(false);

            // Define um tipo de mapa para garantir visibilidade
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            mapaPronto = true;
            Log.d(TAG, "✓ Mapa pronto — dadosProntos=" + dadosProntos);

            if (dadosProntos) {
                plotarRotaOuMostrarVazio();
            }
        } catch (Exception e) {
            Log.e(TAG, "✗ Erro no onMapReady", e);
            mostrarErro("Erro ao carregar o mapa: " + e.getMessage());
        }
    }

    private void plotarRotaOuMostrarVazio() {
        layoutCarregando.setVisibility(View.GONE);
        Log.d(TAG, "→ Plotando rota — trajeto=" + (trajeto != null) + ", mapa=" + (googleMap != null));

        if (trajeto == null || googleMap == null) {
            Log.e(TAG, "✗ Trajeto ou mapa nulo");
            return;
        }

        List<Trajeto.Ponto> pontos = trajeto.getPontos();
        if (pontos == null || pontos.isEmpty()) {
            Log.w(TAG, "✗ Trajeto sem pontos GPS");
            layoutSemDados.setVisibility(View.VISIBLE);
            textSemDados.setText("Este trajeto não possui dados de GPS registrados");
            return;
        }

        Log.d(TAG, "  Total de pontos: " + pontos.size());

        List<LatLng> latLngList = new ArrayList<>();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (Trajeto.Ponto ponto : pontos) {
            if (ponto != null && ponto.getLatitude() != null && ponto.getLongitude() != null) {
                LatLng latlng = new LatLng(ponto.getLatitude(), ponto.getLongitude());
                latLngList.add(latlng);
                boundsBuilder.include(latlng);
                if (latLngList.size() == 1) {
                    Log.d(TAG, "  Primeiro ponto: " + ponto.getLatitude() + ", " + ponto.getLongitude());
                }
            }
        }

        if (latLngList.isEmpty()) {
            Log.e(TAG, "✗ Nenhum ponto com lat/lon válido");
            layoutSemDados.setVisibility(View.VISIBLE);
            textSemDados.setText("Dados de GPS inválidos neste trajeto");
            return;
        }

        Log.d(TAG, "✓ " + latLngList.size() + " pontos válidos prontos para plotar");

        try {
            googleMap.addPolyline(new PolylineOptions()
                    .addAll(latLngList)
                    .width(10f)
                    .color(Color.parseColor("#00D4FF"))
                    .geodesic(true));
            Log.d(TAG, "✓ Polyline adicionada");

            googleMap.addMarker(new MarkerOptions()
                    .position(latLngList.get(0))
                    .title("Início")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            Log.d(TAG, "✓ Marcador de início adicionado");

            LatLng fim = latLngList.get(latLngList.size() - 1);
            googleMap.addMarker(new MarkerOptions()
                    .position(fim)
                    .title("Fim")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            Log.d(TAG, "✓ Marcador de fim adicionado");
        } catch (Exception e) {
            Log.e(TAG, "✗ Erro ao plotar polyline/marcadores", e);
        }

        // setOnMapLoadedCallback só dispara uma vez: se o mapa já estava pronto antes dos
        // dados chegarem, o callback nunca mais dispara e a câmera nunca move.
        // postDelayed garante que a view já está medida e a câmera se moverá corretamente.
        final LatLngBounds bounds;
        try {
            bounds = boundsBuilder.build();
        } catch (Exception e) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngList.get(0), 15));
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed() || googleMap == null) return;
            try {
                if (latLngList.size() == 1) {
                    // trajeto de ponto único — centraliza com zoom fixo
                    googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(latLngList.get(0), 16));
                } else {
                    googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(bounds, 150));
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro ao ajustar câmera", e);
                googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(latLngList.get(0), 15));
            }
        }, 400);
    }

    private void mostrarErro(String mensagem) {
        layoutCarregando.setVisibility(View.GONE);
        layoutSemDados.setVisibility(View.VISIBLE);
        textSemDados.setText(mensagem);
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }
}
