package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.LoginActivity;
import br.jss.motoreviso.activities.SetupPinActivity;
import br.jss.motoreviso.utils.BiometricHelper;
import br.jss.motoreviso.utils.SecurePreferencesManager;

public class ConfiguracoesFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configuracoes, container, false);

        SecurePreferencesManager securePrefs = new SecurePreferencesManager(requireContext());
        FirebaseAuth auth = FirebaseAuth.getInstance();

        TextView textConfiguracao = view.findViewById(R.id.text_configuracao);
        Button btnConfigurarPin = view.findViewById(R.id.btn_configurar_pin);
        Button btnSair = view.findViewById(R.id.btn_sair);

        FirebaseUser user = auth.getCurrentUser();
        String email = user != null ? user.getEmail() : securePrefs.obterEmail();
        String biometriaStatus = BiometricHelper.isBiometriaDisponivel(requireContext())
                ? (securePrefs.isBiometriaHabilitada() ? "Ativada" : "Disponível")
                : "Indisponível";
        String pinStatus = securePrefs.isPinHabilitado() ? "Configurado" : "Não configurado";

        String texto = getString(R.string.configuracoes) + "\n\n"
                + getString(R.string.versao) + "\n\n"
                + getString(R.string.descricao_app) + "\n\n"
                + "Usuário: " + (email != null ? email : "Não identificado") + "\n"
                + "PIN: " + pinStatus + "\n"
                + "Biometria: " + biometriaStatus;
        textConfiguracao.setText(texto);

        if (btnConfigurarPin != null) {
            btnConfigurarPin.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), SetupPinActivity.class)));
        }

        if (btnSair != null) {
            btnSair.setOnClickListener(v -> realizarLogout(auth, securePrefs));
        }

        return view;
    }

    private void realizarLogout(FirebaseAuth auth, SecurePreferencesManager securePrefs) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Sair")
                .setMessage("Deseja realmente sair da conta?")
                .setPositiveButton("Sair", (d, w) -> {
                    auth.signOut();
                    securePrefs.setLoggedIn(false);
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
