package br.jss.motoreviso.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import br.jss.motoreviso.R;
import br.jss.motoreviso.central.CentralPermissions;
import br.jss.motoreviso.central.CentralProtocol;
import br.jss.motoreviso.central.PanelConnection;
import br.jss.motoreviso.central.PanelNotification;
import br.jss.motoreviso.central.PanelNotificationAdapter;

/**
 * Lado PAINEL: central multimídia do guidão. Conecta ao celular pareado e
 * mostra relógio, player de música e notificações em uma tela ~480x480.
 */
public class PainelActivity extends AppCompatActivity implements PanelConnection.Listener {

    private static final String PREFS = "central_prefs";
    private static final String KEY_DEVICE = "panel_device_address";
    private static final String KEY_DEVICE_NAME = "panel_device_name";
    private static final long HEADSUP_MS = 8000;
    private static final long DISCONNECT_GRACE_MS = 1500;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final Locale ptBR = new Locale("pt", "BR");
    private final SimpleDateFormat clockFmt = new SimpleDateFormat("HH:mm", ptBR);
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, d 'de' MMMM", ptBR);

    private SharedPreferences prefs;
    private PanelConnection connection;
    private BluetoothAdapter btAdapter;

    // Estado
    private final LinkedHashMap<String, PanelNotification> notifs = new LinkedHashMap<>();
    private final Map<String, Bitmap> icons = new HashMap<>();
    private PanelNotification headsUpShown;
    private boolean mediaActive, mediaPlaying;
    private long mediaDuration, mediaPosition, mediaRxAt;
    private float mediaSpeed;
    private String mediaTrack;

    // Views
    private View groupIdle, groupMedia, headsUp, overlayList, overlayDisc, stripNotif;
    private TextView tvPhoneName, tvClockSmall, tvBattery, tvClockBig, tvDate;
    private TextView tvMediaApp, tvMediaTitle, tvMediaArtist, tvMediaPos, tvMediaDur;
    private TextView tvStripTitle, tvStripText, tvStripBadge;
    private TextView tvHuApp, tvHuTitle, tvHuText, tvListEmpty, tvDiscTitle, tvDiscSub;
    private ImageView ivArt, ivArtPlaceholder, ivStripIcon, ivHuIcon, ivBt;
    private ImageButton btnPlay, btnPrev, btnNext;
    private ProgressBar pbMedia, pbConnecting;
    private LinearLayout huActions;
    private RecyclerView rvNotifs;
    private PanelNotificationAdapter listAdapter;
    private int actionHeight;

    private final ActivityResultLauncher<String> btPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startOrPick();
                else showDisconnected("Permissão Bluetooth negada",
                        "Conceda a permissão de dispositivos próximos para conectar ao celular.");
            });

    private final ActivityResultLauncher<Intent> btEnable =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> startOrPick());

    // ─────────────────────────────────────────────────────────
    //  Ciclo de vida
    // ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painel);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bm != null ? bm.getAdapter() : null;
        actionHeight = getResources().getDimensionPixelSize(R.dimen.painel_action_h);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
        }
        enterImmersive();
        bindViews();
        setupList();
        setupClicks();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (overlayList.getVisibility() == View.VISIBLE) hideList();
                else if (headsUp.getVisibility() == View.VISIBLE) hideHeadsUp();
                else finish();
            }
        });
        renderMedia();
        renderStrip();
    }

    @Override
    protected void onStart() {
        super.onStart();
        main.post(ticker);
        startOrPick();
    }

    @Override
    protected void onStop() {
        main.removeCallbacks(ticker);
        stopConnection();
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersive();
    }

    private void enterImmersive() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.hide(WindowInsetsCompat.Type.systemBars());
        c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    // ─────────────────────────────────────────────────────────
    //  Conexão
    // ─────────────────────────────────────────────────────────

    private void startOrPick() {
        if (!CentralPermissions.hasBluetooth(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                btPermission.launch(Manifest.permission.BLUETOOTH_CONNECT);
            }
            return;
        }
        if (btAdapter == null) {
            showDisconnected("Bluetooth indisponível", "Este aparelho não possui Bluetooth.");
            return;
        }
        if (!btAdapter.isEnabled()) {
            showDisconnected("Bluetooth desligado", "Ligue o Bluetooth para conectar ao celular.");
            try {
                btEnable.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } catch (SecurityException ignored) {}
            return;
        }
        String address = prefs.getString(KEY_DEVICE, null);
        if (address == null) {
            showDevicePicker();
        } else {
            startConnection(address);
        }
    }

    private void startConnection(String address) {
        if (connection != null) return;
        tvPhoneName.setText(prefs.getString(KEY_DEVICE_NAME, "Celular"));
        connection = new PanelConnection(btAdapter, address, this);
        connection.start();
        onStateChanged(PanelConnection.State.CONNECTING);
    }

    private void stopConnection() {
        if (connection != null) {
            connection.stop();
            connection = null;
        }
    }

    @SuppressLint("MissingPermission")
    private void showDevicePicker() {
        if (!CentralPermissions.hasBluetooth(this) || btAdapter == null) return;
        Set<BluetoothDevice> bonded;
        try {
            bonded = btAdapter.getBondedDevices();
        } catch (SecurityException e) {
            return;
        }
        List<BluetoothDevice> devices = new ArrayList<>(bonded);
        // Celulares primeiro.
        Collections.sort(devices, (a, b) -> Boolean.compare(!isPhone(a), !isPhone(b)));
        if (devices.isEmpty()) {
            showDisconnected("Nenhum celular pareado", "Pareie o celular nas configurações de Bluetooth deste painel.");
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Parear celular")
                    .setMessage("Nenhum aparelho pareado. Abra as configurações de Bluetooth, pareie com o seu celular e volte aqui.")
                    .setPositiveButton("Abrir Bluetooth", (d, w) ->
                            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return;
        }
        String[] names = new String[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            String n = devices.get(i).getName();
            names[i] = (n == null ? devices.get(i).getAddress() : n) + (isPhone(devices.get(i)) ? "  📱" : "");
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Qual é o seu celular?")
                .setItems(names, (d, which) -> {
                    BluetoothDevice dev = devices.get(which);
                    String name = dev.getName();
                    prefs.edit()
                            .putString(KEY_DEVICE, dev.getAddress())
                            .putString(KEY_DEVICE_NAME, name == null ? dev.getAddress() : name)
                            .apply();
                    stopConnection();
                    startConnection(dev.getAddress());
                })
                .setNeutralButton("Parear novo", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)))
                .show();
    }

    @SuppressLint("MissingPermission")
    private static boolean isPhone(BluetoothDevice d) {
        try {
            BluetoothClass c = d.getBluetoothClass();
            return c != null && c.getMajorDeviceClass() == BluetoothClass.Device.Major.PHONE;
        } catch (SecurityException e) {
            return false;
        }
    }

    private void send(String type, String k1, Object v1, String k2, Object v2) {
        if (connection == null) return;
        JSONObject m = CentralProtocol.msg(type);
        try {
            if (k1 != null) m.put(k1, v1);
            if (k2 != null) m.put(k2, v2);
        } catch (JSONException ignored) {}
        connection.send(m);
    }

    // ─────────────────────────────────────────────────────────
    //  PanelConnection.Listener
    // ─────────────────────────────────────────────────────────

    private final Runnable showDiscRunnable = () ->
            showDisconnected("Procurando o celular…",
                    "Abra o Moto Reviso no celular e ative \"Compartilhar com o painel\".");

    @Override
    public void onStateChanged(PanelConnection.State state) {
        if (state == PanelConnection.State.CONNECTED) {
            main.removeCallbacks(showDiscRunnable);
            overlayDisc.setVisibility(View.GONE);
            ivBt.setColorFilter(getColor(R.color.secondary));
            // O celular reenviará o estado completo (snapshot).
            notifs.clear();
            mediaActive = false;
            mediaTrack = null;
            hideHeadsUp();
            renderMedia();
            renderStrip();
        } else {
            ivBt.setColorFilter(getColor(R.color.text_tertiary));
            // Pequena carência para não piscar em reconexões rápidas.
            if (overlayDisc.getVisibility() != View.VISIBLE) {
                main.removeCallbacks(showDiscRunnable);
                main.postDelayed(showDiscRunnable, DISCONNECT_GRACE_MS);
            }
        }
    }

    @Override
    public void onMessage(JSONObject m) {
        switch (m.optString("t")) {
            case CentralProtocol.T_HELLO:
                String name = m.optString("name");
                if (!name.isEmpty()) tvPhoneName.setText(prefs.getString(KEY_DEVICE_NAME, name));
                break;
            case CentralProtocol.T_BATTERY:
                int level = m.optInt("level", -1);
                boolean charging = m.optBoolean("charging");
                tvBattery.setText(level >= 0 ? (charging ? "⚡" : "") + level + "%" : "");
                int color = level >= 0 && level <= 15 && !charging ? R.color.error : R.color.text_secondary;
                tvBattery.setTextColor(getColor(color));
                break;
            case CentralProtocol.T_MEDIA:
                onMedia(m);
                break;
            case CentralProtocol.T_MEDIA_CLEAR:
                mediaActive = false;
                mediaTrack = null;
                renderMedia();
                break;
            case CentralProtocol.T_NOTIF:
                onNotification(m);
                break;
            case CentralProtocol.T_NOTIF_REMOVED:
                removeNotification(m.optString("key"));
                break;
            default:
                break;
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Mídia
    // ─────────────────────────────────────────────────────────

    private void onMedia(JSONObject m) {
        mediaActive = true;
        mediaPlaying = m.optBoolean("playing");
        mediaDuration = m.optLong("duration");
        mediaPosition = m.optLong("position");
        mediaSpeed = (float) m.optDouble("speed", 0);
        mediaRxAt = SystemClock.elapsedRealtime();

        String title = m.optString("title");
        String artist = m.optString("artist");
        String track = title + "|" + artist;
        tvMediaApp.setText(m.optString("app"));
        if (!title.equals(tvMediaTitle.getText().toString())) tvMediaTitle.setText(title);
        tvMediaArtist.setText(artist.isEmpty() ? m.optString("album") : artist);
        tvMediaTitle.setSelected(true); // marquee

        if (m.has("art")) {
            Bitmap art = CentralProtocol.decodeBitmap(m.optString("art"));
            ivArt.setImageBitmap(art);
            ivArtPlaceholder.setVisibility(art == null ? View.VISIBLE : View.GONE);
        } else if (!track.equals(mediaTrack)) {
            ivArt.setImageDrawable(null);
            ivArtPlaceholder.setVisibility(View.VISIBLE);
        }
        mediaTrack = track;

        btnNext.setAlpha(m.optBoolean("canNext", true) ? 1f : 0.35f);
        btnPrev.setAlpha(m.optBoolean("canPrev", true) ? 1f : 0.35f);
        renderMedia();
    }

    private void renderMedia() {
        groupMedia.setVisibility(mediaActive ? View.VISIBLE : View.GONE);
        groupIdle.setVisibility(mediaActive ? View.GONE : View.VISIBLE);
        btnPlay.setImageResource(mediaPlaying ? R.drawable.ic_painel_pause : R.drawable.ic_painel_play);
        updateMediaProgress();
    }

    private void updateMediaProgress() {
        if (!mediaActive) return;
        long pos = mediaPosition;
        if (mediaPlaying) pos += (long) ((SystemClock.elapsedRealtime() - mediaRxAt) * mediaSpeed);
        if (mediaDuration > 0) {
            pos = Math.min(pos, mediaDuration);
            pbMedia.setVisibility(View.VISIBLE);
            pbMedia.setProgress((int) (pos * 1000 / mediaDuration));
            tvMediaDur.setText(formatTime(mediaDuration));
        } else {
            pbMedia.setVisibility(View.INVISIBLE); // rádio / ao vivo
            tvMediaDur.setText("");
        }
        tvMediaPos.setText(mediaDuration > 0 ? formatTime(pos) : (mediaPlaying ? "AO VIVO" : ""));
    }

    private static String formatTime(long ms) {
        long s = ms / 1000;
        return s >= 3600
                ? String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60)
                : String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    // ─────────────────────────────────────────────────────────
    //  Notificações
    // ─────────────────────────────────────────────────────────

    private void onNotification(JSONObject m) {
        PanelNotification n = PanelNotification.from(m);
        if (m.has("icon")) {
            Bitmap icon = CentralProtocol.decodeBitmap(m.optString("icon"));
            if (icon != null) icons.put(n.pkg, icon);
        }
        notifs.remove(n.key); // reinserir no fim = mais recente
        notifs.put(n.key, n);

        boolean alert = m.optBoolean("alert");
        if (alert && !n.isNavigation()) {
            showHeadsUp(n);
        } else if (headsUpShown != null && headsUpShown.key.equals(n.key)) {
            bindHeadsUp(n); // atualiza conteúdo sem reiniciar o timer
        }
        renderStrip();
        refreshListIfVisible();
    }

    private void removeNotification(String key) {
        if (notifs.remove(key) == null) return;
        if (headsUpShown != null && headsUpShown.key.equals(key)) hideHeadsUp();
        renderStrip();
        refreshListIfVisible();
    }

    private List<PanelNotification> sortedNewestFirst() {
        List<PanelNotification> list = new ArrayList<>(notifs.values());
        Collections.reverse(list);
        return list;
    }

    private PanelNotification stripCandidate() {
        PanelNotification latest = null;
        for (PanelNotification n : notifs.values()) {
            if (n.isNavigation()) return n; // navegação fica fixa
            latest = n;
        }
        return latest;
    }

    private void renderStrip() {
        PanelNotification n = stripCandidate();
        if (n == null) {
            ivStripIcon.setImageResource(R.drawable.ic_painel_bell);
            ivStripIcon.setColorFilter(getColor(R.color.text_tertiary));
            tvStripTitle.setText("Sem notificações");
            tvStripText.setText(connection != null && connection.getState() == PanelConnection.State.CONNECTED
                    ? "Tudo tranquilo. Boa viagem!" : "");
            tvStripText.setVisibility(tvStripText.getText().length() == 0 ? View.GONE : View.VISIBLE);
            stripNotif.setBackgroundResource(R.drawable.bg_painel_card);
            tvStripBadge.setVisibility(View.GONE);
            return;
        }
        Bitmap icon = icons.get(n.pkg);
        if (n.isNavigation()) {
            ivStripIcon.setImageResource(R.drawable.ic_painel_navigation);
            ivStripIcon.setColorFilter(getColor(R.color.secondary));
            stripNotif.setBackgroundResource(R.drawable.bg_painel_card_nav);
        } else {
            ivStripIcon.clearColorFilter();
            if (icon != null) ivStripIcon.setImageBitmap(icon);
            else {
                ivStripIcon.setImageResource(R.drawable.ic_painel_bell);
                ivStripIcon.setColorFilter(getColor(R.color.text_secondary));
            }
            stripNotif.setBackgroundResource(R.drawable.bg_painel_card);
        }
        tvStripTitle.setText(n.isNavigation() ? n.title : n.title + "  ·  " + n.app);
        tvStripText.setText(n.text);
        tvStripText.setVisibility(n.text.isEmpty() ? View.GONE : View.VISIBLE);
        int count = notifs.size();
        tvStripBadge.setVisibility(count > 1 ? View.VISIBLE : View.GONE);
        tvStripBadge.setText(String.valueOf(count));
    }

    private void showHeadsUp(PanelNotification n) {
        bindHeadsUp(n);
        main.removeCallbacks(hideHeadsUpRunnable);
        if (headsUp.getVisibility() != View.VISIBLE) {
            headsUp.setVisibility(View.VISIBLE);
            headsUp.setTranslationY(-headsUp.getHeight() - 40f);
            headsUp.setAlpha(0f);
            headsUp.animate().translationY(0).alpha(1f).setDuration(220).start();
        }
        // Chamadas ficam até serem atendidas/recusadas ou removidas.
        if (!n.isCall()) main.postDelayed(hideHeadsUpRunnable, HEADSUP_MS);
    }

    private void bindHeadsUp(PanelNotification n) {
        headsUpShown = n;
        Bitmap icon = icons.get(n.pkg);
        if (icon != null) ivHuIcon.setImageBitmap(icon);
        else ivHuIcon.setImageResource(R.drawable.ic_painel_bell);
        tvHuApp.setText(n.app);
        tvHuTitle.setText(n.title);
        tvHuText.setText(n.text);
        tvHuText.setVisibility(n.text.isEmpty() ? View.GONE : View.VISIBLE);
        n.bindActions(huActions, actionHeight, this::onNotificationAction);
    }

    private final Runnable hideHeadsUpRunnable = this::hideHeadsUp;

    private void hideHeadsUp() {
        main.removeCallbacks(hideHeadsUpRunnable);
        headsUpShown = null;
        if (headsUp.getVisibility() != View.VISIBLE) return;
        headsUp.animate().translationY(-headsUp.getHeight() - 40f).alpha(0f).setDuration(180)
                .withEndAction(() -> headsUp.setVisibility(View.GONE)).start();
    }

    private void onNotificationAction(PanelNotification n, PanelNotification.Action a) {
        send(CentralProtocol.T_NOTIF_ACTION, "key", n.key, "index", a.index);
        if (headsUpShown != null && headsUpShown.key.equals(n.key)) hideHeadsUp();
    }

    private void dismissNotification(PanelNotification n) {
        send(CentralProtocol.T_NOTIF_DISMISS, "key", n.key, null, null);
        removeNotification(n.key);
    }

    // ─────────────────────────────────────────────────────────
    //  Lista
    // ─────────────────────────────────────────────────────────

    private void setupList() {
        listAdapter = new PanelNotificationAdapter(icons, actionHeight, new PanelNotificationAdapter.Callbacks() {
            @Override public void onAction(PanelNotification n, PanelNotification.Action a) { onNotificationAction(n, a); }
            @Override public void onDismiss(PanelNotification n) { dismissNotification(n); }
        });
        rvNotifs.setLayoutManager(new LinearLayoutManager(this));
        rvNotifs.setAdapter(listAdapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) dismissNotification(listAdapter.get(pos));
            }
        }).attachToRecyclerView(rvNotifs);
    }

    private void showList() {
        hideHeadsUp();
        overlayList.setVisibility(View.VISIBLE);
        overlayList.setAlpha(0f);
        overlayList.animate().alpha(1f).setDuration(160).start();
        refreshListIfVisible();
    }

    private void hideList() {
        overlayList.animate().alpha(0f).setDuration(140)
                .withEndAction(() -> overlayList.setVisibility(View.GONE)).start();
    }

    private void refreshListIfVisible() {
        if (overlayList.getVisibility() != View.VISIBLE) return;
        List<PanelNotification> list = sortedNewestFirst();
        listAdapter.submit(list);
        rvNotifs.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        tvListEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ─────────────────────────────────────────────────────────
    //  UI
    // ─────────────────────────────────────────────────────────

    private void showDisconnected(String title, String sub) {
        tvDiscTitle.setText(title);
        tvDiscSub.setText(sub);
        pbConnecting.setVisibility(connection != null ? View.VISIBLE : View.GONE);
        overlayDisc.setVisibility(View.VISIBLE);
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            Date now = new Date();
            String clock = clockFmt.format(now);
            tvClockSmall.setText(clock);
            tvClockBig.setText(clock);
            String date = dateFmt.format(now);
            tvDate.setText(date.substring(0, 1).toUpperCase(ptBR) + date.substring(1));
            updateMediaProgress();
            main.postDelayed(this, 1000);
        }
    };

    private void setupClicks() {
        btnPlay.setOnClickListener(v -> {
            mediaPlaying = !mediaPlaying; // otimista; o celular confirma em seguida
            long now = SystemClock.elapsedRealtime();
            if (mediaPlaying) {
                if (mediaSpeed <= 0) mediaSpeed = 1f;
            } else {
                mediaPosition += (long) ((now - mediaRxAt) * mediaSpeed);
            }
            mediaRxAt = now;
            renderMedia();
            send(CentralProtocol.T_MEDIA_CMD, "cmd", CentralProtocol.CMD_TOGGLE, null, null);
        });
        btnNext.setOnClickListener(v -> send(CentralProtocol.T_MEDIA_CMD, "cmd", CentralProtocol.CMD_NEXT, null, null));
        btnPrev.setOnClickListener(v -> send(CentralProtocol.T_MEDIA_CMD, "cmd", CentralProtocol.CMD_PREV, null, null));

        stripNotif.setOnClickListener(v -> showList());
        headsUp.setOnClickListener(v -> showList());
        findViewById(R.id.btn_hu_close).setOnClickListener(v -> hideHeadsUp());
        findViewById(R.id.btn_list_close).setOnClickListener(v -> hideList());
        findViewById(R.id.btn_list_clear).setOnClickListener(v -> {
            for (PanelNotification n : new ArrayList<>(notifs.values())) {
                if (!n.isCall() && !n.isNavigation()) dismissNotification(n);
            }
        });
        findViewById(R.id.btn_change_device).setOnClickListener(v -> showDevicePicker());
        findViewById(R.id.painel_topbar).setOnLongClickListener(v -> {
            showDevicePicker();
            return true;
        });
    }

    private void bindViews() {
        groupIdle = findViewById(R.id.group_idle);
        groupMedia = findViewById(R.id.group_media);
        headsUp = findViewById(R.id.headsup);
        overlayList = findViewById(R.id.overlay_list);
        overlayDisc = findViewById(R.id.overlay_disconnected);
        stripNotif = findViewById(R.id.strip_notif);
        tvPhoneName = findViewById(R.id.tv_phone_name);
        tvClockSmall = findViewById(R.id.tv_clock_small);
        tvBattery = findViewById(R.id.tv_phone_battery);
        tvClockBig = findViewById(R.id.tv_clock_big);
        tvDate = findViewById(R.id.tv_date);
        tvMediaApp = findViewById(R.id.tv_media_app);
        tvMediaTitle = findViewById(R.id.tv_media_title);
        tvMediaArtist = findViewById(R.id.tv_media_artist);
        tvMediaPos = findViewById(R.id.tv_media_pos);
        tvMediaDur = findViewById(R.id.tv_media_dur);
        tvStripTitle = findViewById(R.id.tv_strip_title);
        tvStripText = findViewById(R.id.tv_strip_text);
        tvStripBadge = findViewById(R.id.tv_strip_badge);
        tvHuApp = findViewById(R.id.tv_hu_app);
        tvHuTitle = findViewById(R.id.tv_hu_title);
        tvHuText = findViewById(R.id.tv_hu_text);
        tvListEmpty = findViewById(R.id.tv_list_empty);
        tvDiscTitle = findViewById(R.id.tv_disc_title);
        tvDiscSub = findViewById(R.id.tv_disc_sub);
        ivArt = findViewById(R.id.iv_art);
        ivArtPlaceholder = findViewById(R.id.iv_art_placeholder);
        ivStripIcon = findViewById(R.id.iv_strip_icon);
        ivHuIcon = findViewById(R.id.iv_hu_icon);
        ivBt = findViewById(R.id.iv_bt);
        btnPlay = findViewById(R.id.btn_play);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        pbMedia = findViewById(R.id.pb_media);
        pbConnecting = findViewById(R.id.pb_connecting);
        huActions = findViewById(R.id.hu_actions);
        rvNotifs = findViewById(R.id.rv_notifs);
    }
}
