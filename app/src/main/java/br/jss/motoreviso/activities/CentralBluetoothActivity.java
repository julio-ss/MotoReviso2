package br.jss.motoreviso.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Map;

import br.jss.motoreviso.R;
import br.jss.motoreviso.central.CentralPermissions;
import br.jss.motoreviso.central.PhoneBridge;
import br.jss.motoreviso.central.PhoneLinkService;
import br.jss.motoreviso.utils.SystemBarHelper;

/**
 * Configuração da Central Bluetooth: escolhe se este aparelho é o celular
 * (compartilha dados) ou o painel do guidão (exibe).
 */
public class CentralBluetoothActivity extends AppCompatActivity {

    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView tvStatusBt, tvStatusNotif, tvShareStatus;
    private SwitchMaterial switchShare;
    private boolean pendingStart;

    private final ActivityResultLauncher<String[]> permissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissions);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central_bluetooth);
        SystemBarHelper.applySystemBarPadding(this, findViewById(android.R.id.content));

        tvStatusBt = findViewById(R.id.tv_status_bt);
        tvStatusNotif = findViewById(R.id.tv_status_notif);
        tvShareStatus = findViewById(R.id.tv_share_status);
        switchShare = findViewById(R.id.switch_share);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.row_perm_bt).setOnClickListener(v -> requestRuntime());
        findViewById(R.id.row_perm_notif).setOnClickListener(v -> openNotificationAccess());
        findViewById(R.id.row_pair).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        findViewById(R.id.row_share).setOnClickListener(v -> toggleShare());
        findViewById(R.id.btn_open_panel).setOnClickListener(v ->
                startActivity(new Intent(this, PainelActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        main.post(refresher);
    }

    @Override
    protected void onPause() {
        main.removeCallbacks(refresher);
        super.onPause();
    }

    private final Runnable refresher = new Runnable() {
        @Override public void run() {
            refresh();
            main.postDelayed(this, 1500);
        }
    };

    private void refresh() {
        boolean bt = CentralPermissions.hasBluetooth(this);
        boolean notif = CentralPermissions.hasNotificationAccess(this);
        setStatus(tvStatusBt, bt, "Conceder");
        setStatus(tvStatusNotif, notif, "Abrir");

        boolean running = PhoneLinkService.isRunning();
        switchShare.setChecked(running);
        if (!running) {
            tvShareStatus.setText("Desligado");
            tvShareStatus.setTextColor(getColor(R.color.text_secondary));
        } else if (PhoneBridge.isPanelConnected()) {
            tvShareStatus.setText("● Conectado ao painel");
            tvShareStatus.setTextColor(getColor(R.color.success));
        } else {
            tvShareStatus.setText(notif ? "Aguardando o painel conectar…" : "Aguardando… (sem acesso a notificações)");
            tvShareStatus.setTextColor(getColor(R.color.warning));
        }
    }

    private void setStatus(TextView tv, boolean ok, String pendingLabel) {
        tv.setText(ok ? "✓ OK" : pendingLabel);
        tv.setTextColor(getColor(ok ? R.color.success : R.color.secondary));
    }

    private void toggleShare() {
        if (PhoneLinkService.isRunning()) {
            PhoneLinkService.stop(this);
            main.postDelayed(this::refresh, 300);
            return;
        }
        if (CentralPermissions.missingRuntime(this).length > 0) {
            pendingStart = true;
            requestRuntime();
            return;
        }
        if (!CentralPermissions.hasNotificationAccess(this)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Acesso a notificações")
                    .setMessage("Para mostrar músicas e notificações no painel, ative o Moto Reviso em \"Acesso a notificações\".")
                    .setPositiveButton("Abrir", (d, w) -> openNotificationAccess())
                    .setNegativeButton("Continuar sem", (d, w) -> startShare())
                    .show();
            return;
        }
        startShare();
    }

    private void startShare() {
        try {
            PhoneLinkService.start(this);
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível iniciar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        main.postDelayed(this::refresh, 300);
    }

    private void requestRuntime() {
        String[] missing = CentralPermissions.missingRuntime(this);
        if (missing.length == 0) {
            refresh();
            return;
        }
        permissions.launch(missing);
    }

    private void onPermissions(Map<String, Boolean> result) {
        refresh();
        if (pendingStart) {
            pendingStart = false;
            if (CentralPermissions.hasBluetooth(this)) toggleShare();
            else Toast.makeText(this, "Permissão de Bluetooth é necessária", Toast.LENGTH_LONG).show();
        }
    }

    private void openNotificationAccess() {
        Intent i;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                            CentralPermissions.listenerComponent(this).flattenToString());
        } else {
            i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        }
        try {
            startActivity(i);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }
}
