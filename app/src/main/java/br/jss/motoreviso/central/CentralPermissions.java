package br.jss.motoreviso.central;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/** Helpers de permissão da Central Bluetooth. */
public final class CentralPermissions {

    private CentralPermissions() {}

    public static boolean hasBluetooth(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasPostNotifications(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Permissões de runtime que ainda faltam (Bluetooth + notificações do app). */
    public static String[] missingRuntime(Context ctx) {
        List<String> list = new ArrayList<>();
        if (!hasBluetooth(ctx)) list.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (!hasPostNotifications(ctx)) list.add(Manifest.permission.POST_NOTIFICATIONS);
        return list.toArray(new String[0]);
    }

    /** "Acesso a notificações" concedido ao {@link PhoneNotificationListener}. */
    public static boolean hasNotificationAccess(Context ctx) {
        return NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.getPackageName());
    }

    public static ComponentName listenerComponent(Context ctx) {
        return new ComponentName(ctx, PhoneNotificationListener.class);
    }
}
