package br.jss.motoreviso.central;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lado PAINEL: cliente RFCOMM que conecta ao celular pareado, reconecta
 * sozinho com backoff e detecta link morto por heartbeat.
 * Todos os callbacks do {@link Listener} chegam na main thread.
 */
public class PanelConnection {

    public enum State { DISCONNECTED, CONNECTING, CONNECTED }

    public interface Listener {
        void onStateChanged(State state);
        void onMessage(JSONObject message);
    }

    private static final String TAG = "CentralPanel";
    private static final long PING_INTERVAL = 4000;
    private static final long DEAD_AFTER = 13000;

    private final BluetoothAdapter adapter;
    private final String address;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService writer = Executors.newSingleThreadExecutor();

    private volatile boolean running;
    private volatile BluetoothSocket socket;
    private volatile DataOutputStream out;
    private volatile long lastRx;
    private volatile State state = State.DISCONNECTED;
    private Thread thread;

    public PanelConnection(BluetoothAdapter adapter, String address, Listener listener) {
        this.adapter = adapter;
        this.address = address;
        this.listener = listener;
    }

    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::loop, "central-panel");
        thread.start();
        main.postDelayed(heartbeat, PING_INTERVAL);
    }

    public void stop() {
        running = false;
        main.removeCallbacks(heartbeat);
        closeSocket();
        if (thread != null) thread.interrupt();
        writer.shutdownNow();
    }

    public State getState() { return state; }

    public void send(JSONObject message) {
        final DataOutputStream o = out;
        if (o == null || writer.isShutdown()) return;
        writer.execute(() -> {
            try {
                CentralProtocol.write(o, message);
            } catch (IOException e) {
                closeSocket();
            }
        });
    }

    // ─────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private void loop() {
        long backoff = 1500;
        while (running) {
            if (adapter == null || !adapter.isEnabled()) {
                setState(State.DISCONNECTED);
                sleep(3000);
                continue;
            }
            setState(State.CONNECTING);
            BluetoothSocket s = null;
            try {
                BluetoothDevice device = adapter.getRemoteDevice(address);
                adapter.cancelDiscovery();
                s = device.createRfcommSocketToServiceRecord(CentralProtocol.SERVICE_UUID);
                socket = s;
                s.connect();
                out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream(), 4096));
                DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream(), 32 * 1024));
                lastRx = SystemClock.elapsedRealtime();
                backoff = 1500;
                setState(State.CONNECTED);

                while (running) {
                    JSONObject msg = CentralProtocol.read(in);
                    lastRx = SystemClock.elapsedRealtime();
                    if (CentralProtocol.T_PONG.equals(msg.optString("t"))) continue;
                    main.post(() -> listener.onMessage(msg));
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Sem permissão Bluetooth", e);
                running = false;
            } catch (IOException | IllegalArgumentException e) {
                Log.i(TAG, "Sem conexão com o celular: " + e.getMessage());
            } finally {
                out = null;
                if (s != null) try { s.close(); } catch (IOException ignored) {}
                socket = null;
            }
            if (!running) break;
            setState(State.DISCONNECTED);
            sleep(backoff);
            backoff = Math.min(backoff * 2, 10000);
        }
        setState(State.DISCONNECTED);
    }

    private final Runnable heartbeat = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            if (state == State.CONNECTED) {
                if (SystemClock.elapsedRealtime() - lastRx > DEAD_AFTER) {
                    Log.i(TAG, "Heartbeat perdido, reconectando");
                    closeSocket();
                } else {
                    send(CentralProtocol.msg(CentralProtocol.T_PING));
                }
            }
            main.postDelayed(this, PING_INTERVAL);
        }
    };

    private void setState(State s) {
        if (state == s) return;
        state = s;
        main.post(() -> listener.onStateChanged(s));
    }

    private void closeSocket() {
        BluetoothSocket s = socket;
        if (s != null) try { s.close(); } catch (IOException ignored) {}
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
