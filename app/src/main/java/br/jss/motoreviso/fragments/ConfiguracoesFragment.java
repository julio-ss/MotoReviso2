package br.jss.motoreviso.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import br.jss.motoreviso.R;

public class ConfiguracoesFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_configuracoes, container, false);

        TextView textConfiguracao = view.findViewById(R.id.text_configuracao);
        textConfiguracao.setText("Configurações do Aplicativo\n\nVersão 1.0\n\nAqui você poderá gerenciar preferências e configurações globais do app.");

        return view;
    }
}