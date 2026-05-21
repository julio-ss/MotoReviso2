package br.jss.motoreviso.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurePreferencesManager {
    private static final String TAG = "SecurePrefsManager";
    private static final String PREFS_FILE = "secure_prefs";

    private static final String KEY_PIN = "pin_hash";
    private static final String KEY_PIN_ENABLED = "pin_enabled";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_LOGGED_IN = "logged_in";

    private final SharedPreferences prefs;

    public SecurePreferencesManager(Context context) {
        SharedPreferences tempPrefs = null;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            tempPrefs = EncryptedSharedPreferences.create(
                    context, PREFS_FILE, masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Erro ao criar EncryptedSharedPreferences, usando prefs normais", e);
            tempPrefs = context.getSharedPreferences(PREFS_FILE + "_fallback", Context.MODE_PRIVATE);
        }
        this.prefs = tempPrefs;
    }

    public void salvarPin(String pin) {
        String hash = hashPin(pin);
        prefs.edit().putString(KEY_PIN, hash).putBoolean(KEY_PIN_ENABLED, true).apply();
    }

    public boolean verificarPin(String pin) {
        String storedHash = prefs.getString(KEY_PIN, null);
        if (storedHash == null) return false;
        return storedHash.equals(hashPin(pin));
    }

    public boolean isPinHabilitado() {
        return prefs.getBoolean(KEY_PIN_ENABLED, false);
    }

    public void habilitarBiometria(boolean habilitar) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, habilitar).apply();
    }

    public boolean isBiometriaHabilitada() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void salvarEmail(String email) {
        prefs.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String obterEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public void setLoggedIn(boolean loggedIn) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void limparDados() {
        prefs.edit().clear().apply();
    }

    private String hashPin(String pin) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((pin + "motoreviso_salt_v1").getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Erro ao fazer hash do PIN", e);
            return pin;
        }
    }
}
