package br.jss.motoreviso.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
import java.util.concurrent.TimeUnit;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Trajeto;

public class MapTrajetoActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MapTrajetoActivity";

    private GoogleMap googleMap;
    private TextView textDistancia, textDuracao, textVelMax;
    private FirebaseManager firebaseManager;
    private String trajetoId;
    private Trajeto trajeto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_trajeto);

        trajetoId = getIntent().getStringExtra("TRAJETO_ID");
        firebaseManager = FirebaseManager.getInstance();

        textDistancia = findViewById(R.id.text_distancia);
        textDuracao = findViewById(R.id.text_duracao);
        textVelMax = findViewById(R.id.text_vel_max);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (trajetoId != null) {
            carregarTrajeto();
        }
    }

    private void carregarTrajeto() {
        Log.d(TAG, "Carregando trajeto: " + trajetoId);
        firebaseManager.obterTrajeto(trajetoId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                trajeto = task.getResult().toObject(Trajeto.class);
                if (trajeto != null) {
                    trajeto.setId(task.getResult().getId());
                    exibirEstatisticas();
                    if (googleMap != null) {
                        plotarRota();
                    }
                }
            } else {
                Log.e(TAG, "Erro ao carregar trajeto", task.getException());
                Toast.makeText(this, "Erro ao carregar trajeto", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void exibirEstatisticas() {
        if (trajeto == null) return;

        Double dist = trajeto.getKmRodados();
        textDistancia.setText(String.format(Locale.getDefault(), "%.1f km", dist != null ? dist : 0.0));

        Long duracao = trajeto.getDuracao();
        if (duracao != null) {
            long min = duracao;
            textDuracao.setText(String.format(Locale.getDefault(), "%dh %02dm", min / 60, min % 60));
        }

        Double velMax = trajeto.getVelocidadeMaxima();
        textVelMax.setText(String.format(Locale.getDefault(), "%.0f km/h", velMax != null ? velMax : 0.0));
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        if (trajeto != null) {
            plotarRota();
        }
    }

    private void plotarRota() {
        if (trajeto == null || googleMap == null) return;

        List<Trajeto.Ponto> pontos = trajeto.getPontos();
        if (pontos == null || pontos.isEmpty()) {
            Toast.makeText(this, "Trajeto sem dados de GPS", Toast.LENGTH_SHORT).show();
            return;
        }

        List<LatLng> latLngList = new ArrayList<>();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (Trajeto.Ponto ponto : pontos) {
            if (ponto.getLatitude() != null && ponto.getLongitude() != null) {
                LatLng latlng = new LatLng(ponto.getLatitude(), ponto.getLongitude());
                latLngList.add(latlng);
                boundsBuilder.include(latlng);
            }
        }

        if (latLngList.isEmpty()) return;

        // Draw polyline
        googleMap.addPolyline(new PolylineOptions()
                .addAll(latLngList)
                .width(8f)
                .color(Color.parseColor("#00D4FF"))
                .geodesic(true));

        // Start marker
        googleMap.addMarker(new MarkerOptions()
                .position(latLngList.get(0))
                .title("Início")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // End marker
        LatLng fim = latLngList.get(latLngList.size() - 1);
        googleMap.addMarker(new MarkerOptions()
                .position(fim)
                .title("Fim")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Fit camera to route
        try {
            LatLngBounds bounds = boundsBuilder.build();
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            Log.e(TAG, "Erro ao ajustar câmera", e);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngList.get(0), 14));
        }

        Log.d(TAG, "Rota plotada com " + latLngList.size() + " pontos");
    }
}
