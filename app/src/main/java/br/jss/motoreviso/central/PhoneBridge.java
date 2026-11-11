package br.jss.motoreviso.central;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Ponte em memória entre o {@link PhoneNotificationListener} (que observa
 * notificações e mídia) e o {@link PhoneLinkService} (que mantém o socket
 * Bluetooth com o painel). Ambos rodam no mesmo processo.
 */
public final class PhoneBridge {

    private static volatile PhoneNotificationListener listener;
    private static volatile PhoneLinkService link;

    /** Pacotes cujo ícone já foi enviado na conexão atual. */
    private static final Set<String> sentIcons = Collections.synchronizedSet(new HashSet<>());

    private PhoneBridge() {}

    static void setListener(PhoneNotificationListener l) { listener = l; }
    static void setLink(PhoneLinkService s) { link = s; }

    public static boolean isListenerConnected() { return listener != null; }

    public static boolean isPanelConnected() {
        PhoneLinkService s = link;
        return s != null && s.isConnected();
    }

    /** Envia ao painel, se houver conexão. */
    static void send(JSONObject message) {
        PhoneLinkService s = link;
        if (s != null) s.send(message);
    }

    /** Chamado quando um painel conecta: zera caches e envia o estado atual. */
    static void onPanelConnected() {
        sentIcons.clear();
        PhoneNotificationListener l = listener;
        if (l != null) l.sendSnapshot();
        else send(CentralProtocol.msg(CentralProtocol.T_SNAPSHOT_END));
    }

    static void onCommand(JSONObject cmd) {
        PhoneNotificationListener l = listener;
        if (l != null) l.handleCommand(cmd);
    }

    /** true se o ícone deste pacote ainda não foi enviado (e marca como enviado). */
    static boolean shouldSendIcon(String pkg) {
        return sentIcons.add(pkg);
    }
}
