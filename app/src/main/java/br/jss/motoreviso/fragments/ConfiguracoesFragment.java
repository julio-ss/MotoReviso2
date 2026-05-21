package br.jss.motoreviso.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import br.jss.motoreviso.BuildConfig;
import br.jss.motoreviso.R;

public class ConfiguracoesFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configuracoes, container, false);

        TextView textConfiguracao = view.findViewById(R.id.text_configuracao);
        String versao = BuildConfig.VERSION_NAME;
        String texto = getString(R.string.configuracoes) + "\n\n"
                + "Versão " + versao + "\n\n"
                + getString(R.string.descricao_app);
        textConfiguracao.setText(texto);

        return view;
    }
}
