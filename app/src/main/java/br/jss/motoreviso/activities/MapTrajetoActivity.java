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

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Trajeto;

public class MapTrajetoActivity extends AppCompatActivity {
    private static final String TAG = "MapTrajetoActivity";

    private MapView mapView;
    private TextView textDistancia, textDuracao, textVelMax, textSemDados;
    private LinearLayout layoutCarregando, layoutSemDados;
    private FirebaseManager firebaseManager;
    private String trajetoId;
    private Trajeto trajeto;

    private boolean mapaPronto = false;
    private boolean dadosProntos = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
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

        mapView = findViewById(R.id.map_view);
        mapView.setMultiTouchControls(true);
        mapaPronto = true;
        Log.d(TAG, "✓ Mapa inicializado");

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

    private void plotarRotaOuMostrarVazio() {
        layoutCarregando.setVisibility(View.GONE);
        Log.d(TAG, "→ Plotando rota — trajeto=" + (trajeto != null) + ", mapa=" + (mapView != null));

        if (trajeto == null || mapView == null) {
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

        final List<GeoPoint> geoPoints = new ArrayList<>();
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (Trajeto.Ponto ponto : pontos) {
            if (ponto != null && ponto.getLatitude() != null && ponto.getLongitude() != null) {
                GeoPoint geoPoint = new GeoPoint(ponto.getLatitude(), ponto.getLongitude());
                geoPoints.add(geoPoint);

                minLat = Math.min(minLat, ponto.getLatitude());
                maxLat = Math.max(maxLat, ponto.getLatitude());
                minLon = Math.min(minLon, ponto.getLongitude());
                maxLon = Math.max(maxLon, ponto.getLongitude());

                if (geoPoints.size() == 1) {
                    Log.d(TAG, "  Primeiro ponto: " + ponto.getLatitude() + ", " + ponto.getLongitude());
                }
            }
        }

        if (geoPoints.isEmpty()) {
            Log.e(TAG, "✗ Nenhum ponto com lat/lon válido");
            layoutSemDados.setVisibility(View.VISIBLE);
            textSemDados.setText("Dados de GPS inválidos neste trajeto");
            return;
        }

        Log.d(TAG, "✓ " + geoPoints.size() + " pontos válidos prontos para plotar");

        try {
            Polyline polyline = new Polyline(mapView);
            polyline.setPoints(geoPoints);
            polyline.setWidth(10f);
            polyline.setColor(Color.parseColor("#00D4FF"));
            mapView.getOverlays().add(polyline);
            Log.d(TAG, "✓ Polyline adicionada");

            Marker markerInicio = new Marker(mapView);
            markerInicio.setPosition(geoPoints.get(0));
            markerInicio.setTitle("Início");
            markerInicio.setSnippet("Ponto inicial do trajeto");
            mapView.getOverlays().add(markerInicio);
            Log.d(TAG, "✓ Marcador de início adicionado");

            Marker markerFim = new Marker(mapView);
            markerFim.setPosition(geoPoints.get(geoPoints.size() - 1));
            markerFim.setTitle("Fim");
            markerFim.setSnippet("Ponto final do trajeto");
            mapView.getOverlays().add(markerFim);
            Log.d(TAG, "✓ Marcador de fim adicionado");
        } catch (Exception e) {
            Log.e(TAG, "✗ Erro ao plotar polyline/marcadores", e);
        }

        final double finalMinLat = minLat;
        final double finalMaxLat = maxLat;
        final double finalMinLon = minLon;
        final double finalMaxLon = maxLon;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing() || isDestroyed() || mapView == null) return;
            try {
                if (geoPoints.size() == 1) {
                    mapView.getController().setZoom(16);
                    mapView.getController().setCenter(geoPoints.get(0));
                } else {
                    double latSpan = finalMaxLat - finalMinLat;
                    double lonSpan = finalMaxLon - finalMinLon;
                    double maxSpan = Math.max(latSpan, lonSpan);

                    int zoom = 15;
                    if (maxSpan < 0.005) zoom = 18;
                    else if (maxSpan < 0.01) zoom = 17;
                    else if (maxSpan < 0.02) zoom = 16;
                    else if (maxSpan < 0.05) zoom = 15;
                    else if (maxSpan < 0.1) zoom = 14;
                    else if (maxSpan < 0.5) zoom = 12;
                    else if (maxSpan < 1.0) zoom = 11;
                    else if (maxSpan < 5.0) zoom = 9;
                    else zoom = 7;

                    double centerLat = (finalMinLat + finalMaxLat) / 2;
                    double centerLon = (finalMinLon + finalMaxLon) / 2;
                    GeoPoint center = new GeoPoint(centerLat, centerLon);

                    mapView.getController().setZoom(zoom);
                    mapView.getController().setCenter(center);
                }
                mapView.invalidate();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao ajustar câmera", e);
                if (!geoPoints.isEmpty()) {
                    mapView.getController().setZoom(15);
                    mapView.getController().setCenter(geoPoints.get(0));
                }
            }
        }, 400);
    }

    private void mostrarErro(String mensagem) {
        layoutCarregando.setVisibility(View.GONE);
        layoutSemDados.setVisibility(View.VISIBLE);
        textSemDados.setText(mensagem);
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            mapView.onDetach();
        }
        super.onDestroy();
    }
}
