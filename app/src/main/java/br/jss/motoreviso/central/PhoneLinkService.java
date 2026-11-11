package br.jss.motoreviso.central;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.CentralBluetoothActivity;

/**
 * Lado CELULAR: serviço em primeiro plano que aguarda o painel do guidão
 * conectar via RFCOMM e mantém o link vivo.
 *
 * O link RFCOMM é independente dos perfis de áudio (A2DP/HFP): o celular
 * pode continuar tocando no fone Bluetooth enquanto conversa com o painel.
 */
public class PhoneLinkService extends Service {

    private static final String TAG = "CentralPhoneLink";
    private static final String CHANNEL_ID = "central_bluetooth";
    private static final int NOTIF_ID = 4810;

    public static final String PREFS = "central_prefs";
    public static final String KEY_PHONE_ENABLED = "phone_enabled";

    private volatile boolean running;
    private volatile boolean connected;
    private volatile BluetoothServerSocket serverSocket;
    private volatile BluetoothSocket socket;
    private volatile DataOutputStream out;
    private volatile String panelName;
    private Thread acceptThread;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();

    private int lastBatteryLevel = -1;
    private boolean lastCharging;

    // ─────────────────────────────────────────────────────────
    //  API estática
    // ─────────────────────────────────────────────────────────

    public static void start(Context ctx) {
        ctx.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_PHONE_ENABLED, true).apply();
        ContextCompat.startForegroundService(ctx, new Intent(ctx, PhoneLinkService.class));
    }

    public static void stop(Context ctx) {
        ctx.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_PHONE_ENABLED, false).apply();
        ctx.stopService(new Intent(ctx, PhoneLinkService.class));
    }

    public static boolean isEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_PHONE_ENABLED, false);
    }

    public static boolean isRunning() {
        return instanceRunning;
    }

    private static volatile boolean instanceRunning;

    // ─────────────────────────────────────────────────────────
    //  Ciclo de vida
    // ─────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE : 0;
        try {
            ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), type);
        } catch (Exception e) {
            // Ex.: permissão BLUETOOTH_CONNECT revogada (Android 14 exige para connectedDevice).
            Log.e(TAG, "Não foi possível iniciar em primeiro plano", e);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!running) {
            running = true;
            instanceRunning = true;
            PhoneBridge.setLink(this);
            registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            acceptThread = new Thread(this::acceptLoop, "central-accept");
            acceptThread.start();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        instanceRunning = false;
        PhoneBridge.setLink(null);
        try { unregisterReceiver(batteryReceiver); } catch (Exception ignored) {}
        closeQuietly();
        if (acceptThread != null) acceptThread.interrupt();
        writer.shutdownNow();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    // ─────────────────────────────────────────────────────────
    //  Conexão
    // ─────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private void acceptLoop() {
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;
        while (running) {
            if (adapter == null || !adapter.isEnabled()) {
                updateNotification();
                sleep(3000);
                continue;
            }
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                        CentralProtocol.SERVICE_NAME, CentralProtocol.SERVICE_UUID);
                updateNotification();
                BluetoothSocket s = serverSocket.accept();
                try { serverSocket.close(); } catch (IOException ignored) {}
                serverSocket = null;
                handleConnection(s);
            } catch (SecurityException e) {
                Log.e(TAG, "Sem permissão Bluetooth", e);
                running = false;
                stopSelf();
            } catch (IOException e) {
                if (running) {
                    Log.w(TAG, "accept falhou, tentando novamente", e);
                    sleep(2000);
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void handleConnection(BluetoothSocket s) {
        socket = s;
        try {
            out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream(), 16 * 1024));
            DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            try {
                panelName = s.getRemoteDevice().getName();
            } catch (SecurityException e) {
                panelName = null;
            }
            connected = true;
            updateNotification();

            JSONObject hello = CentralProtocol.msg(CentralProtocol.T_HELLO);
            hello.put("name", Build.MODEL);
            hello.put("version", CentralProtocol.PROTOCOL_VERSION);
            send(hello);
            lastBatteryLevel = -1;
            sendBattery(registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
            PhoneBridge.onPanelConnected();

            while (running) {
                JSONObject msg = CentralProtocol.read(in);
                String t = msg.optString("t");
                if (CentralProtocol.T_PING.equals(t)) {
                    send(CentralProtocol.msg(CentralProtocol.T_PONG));
                } else if (CentralProtocol.T_SYNC.equals(t)) {
                    PhoneBridge.onPanelConnected();
                } else {
                    PhoneBridge.onCommand(msg);
                }
            }
        } catch (IOException | JSONException e) {
            Log.i(TAG, "Painel desconectou: " + e.getMessage());
        } finally {
            connected = false;
            out = null;
            try { s.close(); } catch (IOException ignored) {}
            socket = null;
            updateNotification();
        }
    }

    boolean isConnected() { return connected; }

    /** Enfileira o envio (thread única, preserva a ordem). */
    void send(JSONObject message) {
        final DataOutputStream o = out;
        if (!connected || o == null || writer.isShutdown()) return;
        writer.execute(() -> {
            try {
                CentralProtocol.write(o, message);
            } catch (IOException e) {
                // Força o read loop a sair e voltar ao accept.
                BluetoothSocket s = socket;
                if (s != null) try { s.close(); } catch (IOException ignored) {}
            }
        });
    }

    private void closeQuietly() {
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    // ─────────────────────────────────────────────────────────
    //  Bateria do celular
    // ─────────────────────────────────────────────────────────

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { sendBattery(intent); }
    };

    private void sendBattery(Intent intent) {
        if (intent == null) return;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        int pct = scale > 0 ? Math.round(level * 100f / scale) : level;
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;
        if (pct == lastBatteryLevel && charging == lastCharging) return;
        lastBatteryLevel = pct;
        lastCharging = charging;
        try {
            JSONObject m = CentralProtocol.msg(CentralProtocol.T_BATTERY);
            m.put("level", pct);
            m.put("charging", charging);
            send(m);
        } catch (JSONException ignored) {}
    }

    // ─────────────────────────────────────────────────────────
    //  Notificação do serviço
    // ─────────────────────────────────────────────────────────

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Central Bluetooth",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Conexão com o painel do guidão");
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        String text;
        if (connected) {
            text = "Conectado ao painel" + (panelName != null ? " · " + panelName : "");
        } else {
            text = "Aguardando o painel do guidão…";
        }
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, CentralBluetoothActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_phone_modern)
                .setContentTitle("Central Bluetooth")
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setContentIntent(open)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && running) {
            try { nm.notify(NOTIF_ID, buildNotification()); } catch (SecurityException ignored) {}
        }
    }
}
