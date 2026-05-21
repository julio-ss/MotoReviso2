package br.jss.motoreviso.managers;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import br.jss.motoreviso.models.Trajeto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocationManager {
    private static final String TAG = "LocationManager";
    private static LocationManager instance;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationListener locationListener;
    private Double velocidadeMaxima = 0.0;
    private Trajeto trajetoAtivo;
    private List<Trajeto.Ponto> pontosRota = new ArrayList<>();

    private LocationManager(Context context) {
        this.fusedLocationProviderClient = LocationServices
                .getFusedLocationProviderClient(context);
    }

    public static synchronized LocationManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocationManager(context);
        }
        return instance;
    }

    public void iniciarRastreamento(Long kmInicial, OnLocationUpdateListener listener) {
        velocidadeMaxima = 0.0;
        pontosRota.clear();

        trajetoAtivo = new Trajeto();
        trajetoAtivo.setDataInicio(System.currentTimeMillis());
        trajetoAtivo.setKmInicial(kmInicial);

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(10)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for (Location location : locationResult.getLocations()) {
                    processarLocacao(location, listener);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(null,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permissão de localização não concedida");
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    public Trajeto pararRastreamento(Long kmFinal) {
        if (locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }

        if (trajetoAtivo != null) {
            trajetoAtivo.setDataFim(System.currentTimeMillis());
            trajetoAtivo.setKmFinal(kmFinal);
            trajetoAtivo.finalizarTrajeto(new Date(), kmFinal, velocidadeMaxima);
            trajetoAtivo.setPontos(pontosRota);
        }

        return trajetoAtivo;
    }

    private void processarLocacao(Location location, OnLocationUpdateListener listener) {
        if (location == null) return;

        Double velocidadeKmh = location.getSpeed() * 3.6;

        if (velocidadeKmh > velocidadeMaxima) {
            velocidadeMaxima = velocidadeKmh;
        }

        Trajeto.Ponto ponto = new Trajeto.Ponto(
                location.getLatitude(),
                location.getLongitude(),
                velocidadeKmh
        );
        pontosRota.add(ponto);

        if (listener != null) {
            listener.onLocationUpdate(
                    location.getLatitude(),
                    location.getLongitude(),
                    velocidadeKmh,
                    velocidadeMaxima
            );
        }

        Log.d(TAG, String.format("Localização: %.6f, %.6f | Velocidade: %.2f km/h | Máx: %.2f km/h",
                location.getLatitude(),
                location.getLongitude(),
                velocidadeKmh,
                velocidadeMaxima
        ));
    }

    public void obterUltimaLocacao(OnLocationUpdateListener listener) {
        if (ActivityCompat.checkSelfPermission(null,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && listener != null) {
                        listener.onLocationUpdate(
                                location.getLatitude(),
                                location.getLongitude(),
                                0.0,
                                0.0
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao obter última localização", e);
                });
    }

    public static double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000;
    }

    public interface OnLocationUpdateListener {
        void onLocationUpdate(Double latitude, Double longitude,
                              Double velocidadeAtual, Double velocidadeMaxima);
    }

    public Trajeto getTrajetoAtivo() {
        return trajetoAtivo;
    }

    public void setTrajetoAtivo(Trajeto trajeto) {
        this.trajetoAtivo = trajeto;
    }

    public Double getVelocidadeMaxima() {
        return velocidadeMaxima;
    }

    public List<Trajeto.Ponto> getPontosRota() {
        return pontosRota;
    }
}