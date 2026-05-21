package br.jss.motoreviso.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import br.jss.motoreviso.services.RastreamentoService;

public class RastreamentoReceiver extends BroadcastReceiver {
    private static final String TAG = "RastreamentoReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        Log.d(TAG, "Ação recebida: " + intent.getAction());

        Intent serviceIntent = new Intent(context, RastreamentoService.class);
        serviceIntent.setAction(intent.getAction());
        context.startService(serviceIntent);
    }
}
