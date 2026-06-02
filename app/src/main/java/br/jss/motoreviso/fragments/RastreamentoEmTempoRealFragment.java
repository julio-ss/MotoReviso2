package br.jss.motoreviso.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import br.jss.motoreviso.R;
import br.jss.motoreviso.activities.MapTrajetoActivity;

public class RastreamentoEmTempoRealFragment extends Fragment {
    private static final String TAG = "RastreamentoEmTempoReal";

    private ProgressBar progressSpeedometer;
    private TextView textVelocidade;
    private TextView textVelMax;
    private TextView textDistancia;
    private TextView textTempo;
    private TextView textAltitude;
    private TextView textInclinacao;
    private TextView textHora;
    private ImageView imgCompass;
    private MaterialButton btnPausar;
    private MaterialButton btnVerMapa;
    private MaterialButton btnHistorico;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rastreamento_tempo_real, container, false);

        inicializarViews(view);
        configurarListeners();
        carregarDadosRastreamento();

        return view;
    }

    private void inicializarViews(View view) {
        progressSpeedometer = view.findViewById(R.id.progress_speedometer);
        textVelocidade = view.findViewById(R.id.text_velocidade);
        textVelMax = view.findViewById(R.id.text_vel_max);
        textDistancia = view.findViewById(R.id.text_distancia);
        textTempo = view.findViewById(R.id.text_tempo);
        textAltitude = view.findViewById(R.id.text_altitude);
        textInclinacao = view.findViewById(R.id.text_inclinacao);
        textHora = view.findViewById(R.id.text_hora);
        imgCompass = view.findViewById(R.id.img_compass);
        btnPausar = view.findViewById(R.id.btn_pausar);
        btnVerMapa = view.findViewById(R.id.btn_ver_mapa);
        btnHistorico = view.findViewById(R.id.btn_historico);
    }

    private void configurarListeners() {
        btnVerMapa.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MapTrajetoActivity.class);
            startActivity(intent);
        });

        btnHistorico.setOnClickListener(v -> {
            // Navegar para o histórico de trajetos
            if (getParentFragmentManager() != null) {
                getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new TrajetosFragment())
                    .addToBackStack(null)
                    .commit();
            }
        });

        btnPausar.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Rastreamento pausado", Toast.LENGTH_SHORT).show();
            btnPausar.setText("Retomar");
        });
    }

    private void carregarDadosRastreamento() {
        // TODO: Implementar carregamento de dados reais do rastreamento
        // Por enquanto, mostra dados de exemplo

        // Atualizar velocidade
        textVelocidade.setText("96");
        progressSpeedometer.setProgress(96);

        // Atualizar hora
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
        textHora.setText(sdf.format(new java.util.Date()));

        // Dados de exemplo
        textVelMax.setText("138");
        textDistancia.setText("42.6");
        textTempo.setText("38:19");
        textAltitude.setText("642");
        textInclinacao.setText("7");
    }
}
