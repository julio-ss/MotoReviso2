package br.jss.motoreviso.central;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Reativa o compartilhamento com o painel após reiniciar o celular, se estava ligado. */
public class CentralBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        if (!PhoneLinkService.isEnabled(context) || !CentralPermissions.hasBluetooth(context)) return;
        try {
            PhoneLinkService.start(context);
        } catch (Exception e) {
            Log.w("CentralBoot", "Não foi possível iniciar no boot", e);
        }
    }
}
