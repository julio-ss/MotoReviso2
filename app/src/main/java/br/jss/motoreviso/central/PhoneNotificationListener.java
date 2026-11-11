package br.jss.motoreviso.central;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lado CELULAR: observa notificações e a sessão de mídia ativa e repassa
 * ao painel via {@link PhoneBridge}. Também executa os comandos vindos do
 * painel (play/pause, próxima faixa, ações de notificação, dispensar).
 *
 * Requer que o usuário conceda "Acesso a notificações" nas configurações.
 */
public class PhoneNotificationListener extends NotificationListenerService {

    private static final String TAG = "CentralListener";
    private static final int MAX_ACTIONS = 3;

    private final Handler main = new Handler(Looper.getMainLooper());
    private MediaSessionManager sessionManager;
    private MediaController controller;
    private String lastArtTrack;
    /** key → hash do conteúdo, para só "alertar" no painel quando algo muda de verdade. */
    private final Map<String, Integer> sentContent = new HashMap<>();

    private final Runnable mediaPush = () -> pushMedia(false);

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsListener =
            controllers -> main.post(this::selectController);

    private final MediaController.Callback mediaCallback = new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { scheduleMediaPush(); }
        @Override public void onMetadataChanged(MediaMetadata metadata) { scheduleMediaPush(); }
        @Override public void onSessionDestroyed() { main.post(() -> selectController()); }
    };

    // ─────────────────────────────────────────────────────────
    //  Ciclo de vida
    // ─────────────────────────────────────────────────────────

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        PhoneBridge.setListener(this);
        sessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, selfComponent());
        } catch (SecurityException e) {
            Log.w(TAG, "Sem acesso às sessões de mídia", e);
        }
        selectController();
    }

    @Override
    public void onListenerDisconnected() {
        PhoneBridge.setListener(null);
        if (sessionManager != null) {
            try { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener); } catch (Exception ignored) {}
        }
        setController(null);
        super.onListenerDisconnected();
    }

    private ComponentName selfComponent() {
        return new ComponentName(this, PhoneNotificationListener.class);
    }

    // ─────────────────────────────────────────────────────────
    //  Mídia
    // ─────────────────────────────────────────────────────────

    private void selectController() {
        if (sessionManager == null) return;
        List<MediaController> list;
        try {
            list = sessionManager.getActiveSessions(selfComponent());
        } catch (SecurityException e) {
            return;
        }
        MediaController best = null;
        for (MediaController c : list) {
            PlaybackState s = c.getPlaybackState();
            if (s != null && s.getState() == PlaybackState.STATE_PLAYING) { best = c; break; }
        }
        if (best == null && !list.isEmpty()) best = list.get(0);
        setController(best);
        scheduleMediaPush();
    }

    private void setController(MediaController c) {
        if (controller != null && (c == null || !controller.getSessionToken().equals(c.getSessionToken()))) {
            controller.unregisterCallback(mediaCallback);
        } else if (controller != null) {
            return; // mesma sessão, já registrada
        }
        controller = c;
        if (c != null) c.registerCallback(mediaCallback, main);
    }

    private void scheduleMediaPush() {
        // Alguns players disparam callbacks várias vezes por segundo: agrupa.
        main.removeCallbacks(mediaPush);
        main.postDelayed(mediaPush, 250);
    }

    private void pushMedia(boolean forceArt) {
        MediaController c = controller;
        MediaMetadata md = c != null ? c.getMetadata() : null;
        PlaybackState ps = c != null ? c.getPlaybackState() : null;
        if (c == null || md == null || ps == null || ps.getState() == PlaybackState.STATE_NONE
                || ps.getState() == PlaybackState.STATE_STOPPED) {
            lastArtTrack = null;
            PhoneBridge.send(CentralProtocol.msg(CentralProtocol.T_MEDIA_CLEAR));
            return;
        }
        try {
            JSONObject m = CentralProtocol.msg(CentralProtocol.T_MEDIA);
            String title = str(md.getText(MediaMetadata.METADATA_KEY_TITLE));
            String artist = str(md.getText(MediaMetadata.METADATA_KEY_ARTIST));
            if (artist.isEmpty()) artist = str(md.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));
            String album = str(md.getText(MediaMetadata.METADATA_KEY_ALBUM));
            boolean playing = ps.getState() == PlaybackState.STATE_PLAYING
                    || ps.getState() == PlaybackState.STATE_BUFFERING;

            long position = ps.getPosition();
            if (playing && ps.getLastPositionUpdateTime() > 0) {
                position += (long) ((SystemClock.elapsedRealtime() - ps.getLastPositionUpdateTime())
                        * ps.getPlaybackSpeed());
            }
            long duration = md.getLong(MediaMetadata.METADATA_KEY_DURATION);
            if (duration > 0) position = Math.min(position, duration);

            m.put("playing", playing);
            m.put("title", title);
            m.put("artist", artist);
            m.put("album", album);
            m.put("app", appLabel(c.getPackageName()));
            m.put("duration", duration);
            m.put("position", Math.max(0, position));
            m.put("speed", playing ? ps.getPlaybackSpeed() : 0f);
            long actions = ps.getActions();
            m.put("canNext", (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);
            m.put("canPrev", (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0);

            String track = title + "|" + artist + "|" + album;
            if (forceArt || !track.equals(lastArtTrack)) {
                Bitmap art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (art == null) art = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if (art == null) art = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                // Se a capa ainda não chegou, tenta de novo no próximo callback.
                if (art != null) {
                    lastArtTrack = track;
                    m.put("art", CentralProtocol.encodeBitmap(art, 240, false));
                } else {
                    m.put("artMissing", true);
                }
            }
            PhoneBridge.send(m);
        } catch (JSONException e) {
            Log.w(TAG, "Falha ao montar mídia", e);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Notificações
    // ─────────────────────────────────────────────────────────

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!isRelevant(sbn)) return;
        JSONObject msg = buildNotification(sbn, true);
        if (msg != null) PhoneBridge.send(msg);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sentContent.remove(sbn.getKey()) == null) return;
        try {
            JSONObject m = CentralProtocol.msg(CentralProtocol.T_NOTIF_REMOVED);
            m.put("key", sbn.getKey());
            PhoneBridge.send(m);
        } catch (JSONException ignored) {}
    }

    private boolean isRelevant(StatusBarNotification sbn) {
        if (getPackageName().equals(sbn.getPackageName())) return false;
        Notification n = sbn.getNotification();
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return false;
        if (n.extras != null && n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return false;
        String cat = n.category;
        boolean pinned = Notification.CATEGORY_CALL.equals(cat) || Notification.CATEGORY_NAVIGATION.equals(cat);
        if (sbn.isOngoing() && !pinned) return false;
        if (Notification.CATEGORY_PROGRESS.equals(cat) || Notification.CATEGORY_SERVICE.equals(cat)
                || Notification.CATEGORY_TRANSPORT.equals(cat)) return false;
        return !(TextUtils.isEmpty(title(n)) && TextUtils.isEmpty(text(n)));
    }

    private JSONObject buildNotification(StatusBarNotification sbn, boolean live) {
        Notification n = sbn.getNotification();
        try {
            String title = title(n);
            String text = text(n);
            int hash = Arrays.hashCode(new Object[]{title, text});
            Integer prev = sentContent.put(sbn.getKey(), hash);
            boolean changed = prev == null || prev != hash;
            if (live && !changed && !Notification.CATEGORY_NAVIGATION.equals(n.category)) {
                return null; // repost idêntico (ex.: app atualizando timestamp)
            }
            boolean alertOnce = (n.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0;

            JSONObject m = CentralProtocol.msg(CentralProtocol.T_NOTIF);
            m.put("key", sbn.getKey());
            m.put("pkg", sbn.getPackageName());
            m.put("app", appLabel(sbn.getPackageName()));
            m.put("title", title);
            m.put("text", text);
            m.put("time", sbn.getPostTime());
            m.put("category", n.category == null ? "" : n.category);
            m.put("alert", live && changed && !(alertOnce && prev != null));

            JSONArray actions = new JSONArray();
            if (n.actions != null) {
                for (int i = 0; i < n.actions.length && actions.length() < MAX_ACTIONS; i++) {
                    Notification.Action a = n.actions[i];
                    // Ações com campo de texto (responder) não fazem sentido no guidão.
                    if (a == null || a.title == null || a.actionIntent == null) continue;
                    if (a.getRemoteInputs() != null && a.getRemoteInputs().length > 0) continue;
                    JSONObject ja = new JSONObject();
                    ja.put("index", i);
                    ja.put("title", a.title.toString());
                    actions.put(ja);
                }
            }
            m.put("actions", actions);

            if (PhoneBridge.shouldSendIcon(sbn.getPackageName())) {
                try {
                    m.put("icon", CentralProtocol.encodeDrawable(
                            getPackageManager().getApplicationIcon(sbn.getPackageName()), 72));
                } catch (PackageManager.NameNotFoundException ignored) {}
            }
            return m;
        } catch (JSONException e) {
            return null;
        }
    }

    /** Envia todas as notificações ativas + mídia para um painel recém-conectado. */
    void sendSnapshot() {
        main.post(() -> {
            sentContent.clear();
            lastArtTrack = null;
            List<StatusBarNotification> list = new ArrayList<>();
            try {
                StatusBarNotification[] active = getActiveNotifications();
                if (active != null) list.addAll(Arrays.asList(active));
            } catch (Exception e) {
                Log.w(TAG, "getActiveNotifications falhou", e);
            }
            list.sort((a, b) -> Long.compare(a.getPostTime(), b.getPostTime()));
            for (StatusBarNotification sbn : list) {
                if (!isRelevant(sbn)) continue;
                JSONObject m = buildNotification(sbn, false);
                if (m != null) PhoneBridge.send(m);
            }
            selectController();
            pushMedia(true);
            PhoneBridge.send(CentralProtocol.msg(CentralProtocol.T_SNAPSHOT_END));
        });
    }

    // ─────────────────────────────────────────────────────────
    //  Comandos vindos do painel
    // ─────────────────────────────────────────────────────────

    void handleCommand(JSONObject cmd) {
        main.post(() -> {
            String t = cmd.optString("t");
            switch (t) {
                case CentralProtocol.T_MEDIA_CMD:
                    mediaCommand(cmd.optString("cmd"));
                    break;
                case CentralProtocol.T_NOTIF_ACTION:
                    notificationAction(cmd.optString("key"), cmd.optInt("index", -1));
                    break;
                case CentralProtocol.T_NOTIF_OPEN:
                    notificationOpen(cmd.optString("key"));
                    break;
                case CentralProtocol.T_NOTIF_DISMISS:
                    try { cancelNotification(cmd.optString("key")); } catch (Exception ignored) {}
                    break;
                default:
                    break;
            }
        });
    }

    private void mediaCommand(String c) {
        if (controller == null) selectController();
        if (controller == null) return;
        MediaController.TransportControls tc = controller.getTransportControls();
        PlaybackState ps = controller.getPlaybackState();
        switch (c) {
            case CentralProtocol.CMD_TOGGLE:
                if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) tc.pause();
                else tc.play();
                break;
            case CentralProtocol.CMD_NEXT:
                tc.skipToNext();
                break;
            case CentralProtocol.CMD_PREV:
                tc.skipToPrevious();
                break;
            default:
                break;
        }
    }

    private StatusBarNotification findActive(String key) {
        try {
            StatusBarNotification[] arr = getActiveNotifications(new String[]{key});
            return arr != null && arr.length > 0 ? arr[0] : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void notificationAction(String key, int index) {
        StatusBarNotification sbn = findActive(key);
        if (sbn == null) return;
        Notification.Action[] actions = sbn.getNotification().actions;
        if (actions == null || index < 0 || index >= actions.length) return;
        sendPending(actions[index].actionIntent);
    }

    private void notificationOpen(String key) {
        StatusBarNotification sbn = findActive(key);
        if (sbn != null) sendPending(sbn.getNotification().contentIntent);
    }

    private void sendPending(PendingIntent pi) {
        if (pi == null) return;
        try {
            pi.send();
        } catch (PendingIntent.CanceledException e) {
            Log.w(TAG, "PendingIntent cancelado", e);
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Utilitários
    // ─────────────────────────────────────────────────────────

    private static String title(Notification n) {
        Bundle e = n.extras;
        if (e == null) return "";
        CharSequence conv = e.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE);
        CharSequence t = e.getCharSequence(Notification.EXTRA_TITLE);
        if (!TextUtils.isEmpty(conv) && !TextUtils.isEmpty(t) && !conv.toString().equals(t.toString())) {
            return conv + " · " + t;
        }
        return str(t != null ? t : conv);
    }

    private static String text(Notification n) {
        Bundle e = n.extras;
        if (e == null) return "";
        CharSequence big = e.getCharSequence(Notification.EXTRA_BIG_TEXT);
        CharSequence t = e.getCharSequence(Notification.EXTRA_TEXT);
        String s = str(!TextUtils.isEmpty(big) ? big : t);
        return s.length() > 400 ? s.substring(0, 400) + "…" : s;
    }

    private String appLabel(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private static String str(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }
}
