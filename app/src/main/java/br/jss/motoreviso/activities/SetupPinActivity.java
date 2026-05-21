package br.jss.motoreviso.activities;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import br.jss.motoreviso.R;
import br.jss.motoreviso.utils.BiometricHelper;
import br.jss.motoreviso.utils.SecurePreferencesManager;

public class SetupPinActivity extends AppCompatActivity {

    private TextInputEditText edtNovoPin, edtConfirmarPin;
    private CheckBox checkboxBiometria;
    private MaterialButton btnSalvarPin, btnPular;
    private SecurePreferencesManager securePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_pin);

        securePrefs = new SecurePreferencesManager(this);

        edtNovoPin = findViewById(R.id.edt_novo_pin);
        edtConfirmarPin = findViewById(R.id.edt_confirmar_pin);
        checkboxBiometria = findViewById(R.id.checkbox_biometria);
        btnSalvarPin = findViewById(R.id.btn_salvar_pin);
        btnPular = findViewById(R.id.btn_pular);

        checkboxBiometria.setEnabled(BiometricHelper.isBiometriaDisponivel(this));
        if (!BiometricHelper.isBiometriaDisponivel(this)) {
            checkboxBiometria.setText("Biometria não disponível neste dispositivo");
        }

        btnSalvarPin.setOnClickListener(v -> salvarPin());
        btnPular.setOnClickListener(v -> finish());
    }

    private void salvarPin() {
        String pin = edtNovoPin.getText() != null ? edtNovoPin.getText().toString() : "";
        String confirmar = edtConfirmarPin.getText() != null ? edtConfirmarPin.getText().toString() : "";

        if (pin.length() < 4) {
            Toast.makeText(this, "PIN deve ter ao menos 4 dígitos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!pin.equals(confirmar)) {
            Toast.makeText(this, "PINs não coincidem", Toast.LENGTH_SHORT).show();
            edtConfirmarPin.setText("");
            return;
        }

        securePrefs.salvarPin(pin);
        securePrefs.habilitarBiometria(checkboxBiometria.isChecked());

        Toast.makeText(this, "PIN configurado com sucesso!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
