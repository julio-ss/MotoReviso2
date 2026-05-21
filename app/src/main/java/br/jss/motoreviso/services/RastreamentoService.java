package br.jss.motoreviso.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.RastreamentoActivity;
import br.jss.motoreviso.models.Trajeto;
import br.jss.motoreviso.receivers.RastreamentoReceiver;

public class RastreamentoService extends Service {
    private static final String TAG = "RastreamentoService";
    public static final String CHANNEL_ID = "rastreamento_channel";
    public static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_PAUSE = "br.jss.motoreviso.PAUSE";
    public static final String ACTION_RESUME = "br.jss.motoreviso.RESUME";
    public static final String ACTION_STOP = "br.jss.motoreviso.STOP";

    public static final String BROADCAST_UPDATE = "br.jss.motoreviso.TRACKING_UPDATE";
    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_VELOCIDADE = "velocidade";
    public static final String EXTRA_VELOCIDADE_MAX = "velocidade_max";
    public static final String EXTRA_DISTANCIA = "distancia";
    public static final String EXTRA_TEMPO = "tempo";
    public static final String EXTRA_PAUSADO = "pausado";

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private NotificationManager notificationManager;

    private boolean pausado = false;
    private boolean rastreando = false;
    private long tempoInicio = 0;
    private long tempoDecorrido = 0;
    private long tempoPausa = 0;
    private double distanciaTotal = 0;
    private double velocidadeMaxima = 0;
    private Location ultimaLocalizacao = null;

    private List<Trajeto.Ponto> pontosRota = new ArrayList<>();
    private String veiculoId;
    private Long kmInicial;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public RastreamentoService getService() {
            return RastreamentoService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        criarCanalNotificacao();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        Log.d(TAG, "onStartCommand: " + action);

        if (ACTION_PAUSE.equals(action)) {
            pausarRastreamento();
        } else if (ACTION_RESUME.equals(action)) {
            retomarRastreamento();
        } else if (ACTION_STOP.equals(action)) {
            pararRastreamento();
        } else {
            veiculoId = intent.getStringExtra("VEICULO_ID");
            kmInicial = intent.getLongExtra("KM_INICIAL", 0L);
            iniciarRastreamento();
        }

        return START_STICKY;
    }

    private void iniciarRastreamento() {
        if (rastreando) return;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permissão de localização não concedida");
            stopSelf();
            return;
        }

        rastreando = true;
        pausado = false;
        tempoInicio = System.currentTimeMillis();
        tempoDecorrido = 0;
        distanciaTotal = 0;
        velocidadeMaxima = 0;
        pontosRota.clear();
        ultimaLocalizacao = null;

        Log.d(TAG, "Iniciando rastreamento foreground service");

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateDistanceMeters(5)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                for (Location location : result.getLocations()) {
                    processarLocalizacao(location);
                }
            }
        };

        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Erro de segurança ao iniciar GPS", e);
            stopSelf();
            return;
        }

        startForeground(NOTIFICATION_ID, construirNotificacao("0:00", "0.00 km", "0 km/h", false));
    }

    private void processarLocalizacao(Location location) {
        if (pausado || !rastreando) return;

        double velocidadeKmh = location.getSpeed() >= 0 ? location.getSpeed() * 3.6 : 0.0;
        if (velocidadeKmh > velocidadeMaxima) velocidadeMaxima = velocidadeKmh;

        if (ultimaLocalizacao != null) {
            float[] result = new float[1];
            Location.distanceBetween(ultimaLocalizacao.getLatitude(), ultimaLocalizacao.getLongitude(),
                    location.getLatitude(), location.getLongitude(), result);
            distanciaTotal += result[0] / 1000.0;
        }
        ultimaLocalizacao = location;

        pontosRota.add(new Trajeto.Ponto(location.getLatitude(), location.getLongitude(), velocidadeKmh));

        tempoDecorrido = System.currentTimeMillis() - tempoInicio - tempoPausa;
        long segundos = TimeUnit.MILLISECONDS.toSeconds(tempoDecorrido);
        long min = segundos / 60;
        long seg = segundos % 60;
        String tempoStr = String.format(Locale.getDefault(), "%d:%02d", min, seg);
        String distStr = String.format(Locale.getDefault(), "%.2f km", distanciaTotal);
        String velStr = String.format(Locale.getDefault(), "%.0f km/h", velocidadeKmh);

        atualizarNotificacao(tempoStr, distStr, velStr, false);
        enviarBroadcastUpdate(location.getLatitude(), location.getLongitude(),
                velocidadeKmh, velocidadeMaxima, distanciaTotal, tempoDecorrido, false);
    }

    private void pausarRastreamento() {
        if (!rastreando || pausado) return;
        pausado = true;
        tempoPausa = System.currentTimeMillis() - tempoInicio - tempoDecorrido;

        Log.d(TAG, "Rastreamento pausado");
        atualizarNotificacao(formatarTempo(tempoDecorrido),
                String.format(Locale.getDefault(), "%.2f km", distanciaTotal),
                "0 km/h", true);
        enviarBroadcastUpdate(0, 0, 0, velocidadeMaxima, distanciaTotal, tempoDecorrido, true);
    }

    private void retomarRastreamento() {
        if (!rastreando || !pausado) return;
        pausado = false;
        long agora = System.currentTimeMillis();
        tempoPausa = agora - tempoInicio - tempoDecorrido;

        Log.d(TAG, "Rastreamento retomado");
        atualizarNotificacao(formatarTempo(tempoDecorrido),
                String.format(Locale.getDefault(), "%.2f km", distanciaTotal),
                "0 km/h", false);
        enviarBroadcastUpdate(0, 0, 0, velocidadeMaxima, distanciaTotal, tempoDecorrido, false);
    }

    public void pararRastreamento() {
        Log.d(TAG, "Parando rastreamento");
        rastreando = false;
        pausado = false;

        if (locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }

        stopForeground(true);
        stopSelf();
    }

    public Trajeto finalizarETrajeto() {
        Trajeto trajeto = new Trajeto();
        trajeto.setVeiculoId(veiculoId);
        trajeto.setDataInicio(tempoInicio);
        trajeto.setDataFim(System.currentTimeMillis());
        trajeto.setKmInicial(kmInicial);
        trajeto.setKmFinal(kmInicial != null ? kmInicial + (long) distanciaTotal : 0L);
        trajeto.setDistanciaKm(distanciaTotal);
        trajeto.setDuracaoMs(tempoDecorrido);
        trajeto.setVelocidadeMaxima(velocidadeMaxima);
        trajeto.setPontos(new ArrayList<>(pontosRota));
        return trajeto;
    }

    private void enviarBroadcastUpdate(double lat, double lon, double vel, double velMax,
                                        double dist, long tempo, boolean isPausado) {
        Intent intent = new Intent(BROADCAST_UPDATE);
        intent.putExtra(EXTRA_LATITUDE, lat);
        intent.putExtra(EXTRA_LONGITUDE, lon);
        intent.putExtra(EXTRA_VELOCIDADE, vel);
        intent.putExtra(EXTRA_VELOCIDADE_MAX, velMax);
        intent.putExtra(EXTRA_DISTANCIA, dist);
        intent.putExtra(EXTRA_TEMPO, tempo);
        intent.putExtra(EXTRA_PAUSADO, isPausado);
        sendBroadcast(intent);
    }

    private Notification construirNotificacao(String tempo, String distancia, String velocidade, boolean isPausado) {
        Intent openIntent = new Intent(this, RastreamentoActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, RastreamentoReceiver.class);
        pauseIntent.setAction(isPausado ? ACTION_RESUME : ACTION_PAUSE);
        PendingIntent pausePending = PendingIntent.getBroadcast(this, 1, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, RastreamentoReceiver.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getBroadcast(this, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_location_modern)
                .setContentTitle("Rastreamento ativo")
                .setContentText(distancia + " • " + tempo + " • " + velocidade)
                .setContentIntent(openPending)
                .addAction(isPausado ? R.drawable.ic_play_modern : R.drawable.ic_stop_modern,
                        isPausado ? "Continuar" : "Pausar", pausePending)
                .addAction(R.drawable.ic_close_modern, "Encerrar", stopPending)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    private void atualizarNotificacao(String tempo, String distancia, String velocidade, boolean isPausado) {
        notificationManager.notify(NOTIFICATION_ID, construirNotificacao(tempo, distancia, velocidade, isPausado));
    }

    private void criarCanalNotificacao() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Rastreamento de percurso",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Notificação de rastreamento em andamento");
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
    }

    private String formatarTempo(long ms) {
        long seg = TimeUnit.MILLISECONDS.toSeconds(ms);
        return String.format(Locale.getDefault(), "%d:%02d", seg / 60, seg % 60);
    }

    public boolean isPausado() { return pausado; }
    public boolean isRastreando() { return rastreando; }
    public double getDistanciaTotal() { return distanciaTotal; }
    public long getTempoDecorrido() { return tempoDecorrido; }
    public List<Trajeto.Ponto> getPontosRota() { return new ArrayList<>(pontosRota); }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        Log.d(TAG, "Serviço de rastreamento destruído");
    }
}
