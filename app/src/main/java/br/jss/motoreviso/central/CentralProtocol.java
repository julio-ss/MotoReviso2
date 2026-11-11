package br.jss.motoreviso.central;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Protocolo da Central Bluetooth (celular ⇄ painel do guidão).
 *
 * Transporte: RFCOMM (Bluetooth clássico). Cada mensagem é um JSON UTF-8
 * precedido por um int (4 bytes, big-endian) com o tamanho.
 *
 * O tráfego é propositalmente pequeno (capa do álbum só quando a faixa muda,
 * ícones de app só uma vez por conexão) para coexistir com fones A2DP no
 * mesmo celular sem engasgar o áudio.
 */
public final class CentralProtocol {

    private CentralProtocol() {}

    /** UUID do serviço SDP. O mesmo nos dois lados. */
    public static final UUID SERVICE_UUID = UUID.fromString("6f7c1a52-3b4e-4d2a-9a5f-8e2b0c7d4a11");
    public static final String SERVICE_NAME = "MotoRevisoCentral";
    public static final int PROTOCOL_VERSION = 1;

    private static final int MAX_FRAME = 512 * 1024;

    // ── Celular → Painel ───────────────────────────────
    public static final String T_HELLO = "hello";           // {name, version}
    public static final String T_BATTERY = "battery";       // {level, charging}
    public static final String T_MEDIA = "media";           // {active, playing, title, artist, album, app, duration, position, speed, art?}
    public static final String T_MEDIA_CLEAR = "media_clear";
    public static final String T_NOTIF = "notif";           // {key, pkg, app, title, text, time, category, actions[], icon?}
    public static final String T_NOTIF_REMOVED = "notif_removed"; // {key}
    public static final String T_SNAPSHOT_END = "snapshot_end";
    public static final String T_PONG = "pong";

    // ── Painel → Celular ───────────────────────────────
    public static final String T_PING = "ping";
    public static final String T_SYNC = "sync";
    public static final String T_MEDIA_CMD = "media_cmd";   // {cmd: play|pause|toggle|next|prev}
    public static final String T_NOTIF_ACTION = "notif_action"; // {key, index}
    public static final String T_NOTIF_OPEN = "notif_open";     // {key}
    public static final String T_NOTIF_DISMISS = "notif_dismiss"; // {key}

    public static final String CMD_TOGGLE = "toggle";
    public static final String CMD_NEXT = "next";
    public static final String CMD_PREV = "prev";

    public static JSONObject msg(String type) {
        JSONObject o = new JSONObject();
        try {
            o.put("t", type);
        } catch (JSONException ignored) {}
        return o;
    }

    public static void write(DataOutputStream out, JSONObject message) throws IOException {
        byte[] data = message.toString().getBytes(StandardCharsets.UTF_8);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    public static JSONObject read(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len <= 0 || len > MAX_FRAME) throw new IOException("Frame inválido: " + len);
        byte[] buf = new byte[len];
        in.readFully(buf);
        try {
            return new JSONObject(new String(buf, StandardCharsets.UTF_8));
        } catch (JSONException e) {
            throw new IOException("JSON inválido", e);
        }
    }

    // ── Imagens ────────────────────────────────────────

    public static String encodeBitmap(Bitmap src, int maxSize, boolean png) {
        if (src == null) return null;
        try {
            Bitmap scaled = scale(src, maxSize);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            scaled.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 75, bos);
            return Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    public static String encodeDrawable(Drawable d, int size) {
        if (d == null) return null;
        try {
            if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getBitmap() != null) {
                return encodeBitmap(((BitmapDrawable) d).getBitmap(), size, true);
            }
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, size, size);
            d.draw(c);
            return encodeBitmap(bmp, size, true);
        } catch (Exception e) {
            return null;
        }
    }

    public static Bitmap decodeBitmap(String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        try {
            byte[] data = Base64.decode(b64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap scale(Bitmap src, int maxSize) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxSize && h <= maxSize) return src;
        float r = Math.min((float) maxSize / w, (float) maxSize / h);
        return Bitmap.createScaledBitmap(src, Math.max(1, Math.round(w * r)), Math.max(1, Math.round(h * r)), true);
    }
}
