package br.jss.motoreviso.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import br.jss.motoreviso.R;
import br.jss.motoreviso.managers.FirebaseManager;
import br.jss.motoreviso.models.Veiculo;

public class EspecificacoesFragment extends Fragment {
    private String veiculoId;
    private ScrollView scrollView;
    private ProgressBar progressBar;
    private FirebaseManager firebaseManager;

    public EspecificacoesFragment(String veiculoId) {
        this.veiculoId = veiculoId;
    }

    public EspecificacoesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_especificacoes, container, false);

        scrollView = view.findViewById(R.id.scroll_view);
        progressBar = view.findViewById(R.id.progress_bar);

        if (veiculoId == null) {
            veiculoId = getArguments() != null ? getArguments().getString("VEICULO_ID") : null;
        }

        firebaseManager = FirebaseManager.getInstance();

        if (veiculoId != null) {
            carregarEspecificacoes(view);
        }

        return view;
    }

    private void carregarEspecificacoes(View view) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseManager.obterVeiculo(veiculoId).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                Veiculo veiculo = task.getResult().toObject(Veiculo.class);
                if (veiculo != null) {
                    exibirEspecificacoes(view, veiculo);
                }
            }
        });
    }

    private void exibirEspecificacoes(View view, Veiculo veiculo) {
        StringBuilder sb = new StringBuilder();
        sb.append("ESPECIFICAÇÕES DO VEÍCULO\n\n");
        sb.append("Marca: ").append(veiculo.getMarca() != null ? veiculo.getMarca() : "N/A").append("\n");
        sb.append("Modelo: ").append(veiculo.getModelo() != null ? veiculo.getModelo() : "N/A").append("\n");
        sb.append("Ano: ").append(veiculo.getAno() != null ? veiculo.getAno() : "N/A").append("\n");
        sb.append("Placa: ").append(veiculo.getPlaca() != null ? veiculo.getPlaca() : "N/A").append("\n");
        sb.append("\nDETALHES TÉCNICOS\n");
        sb.append("Combustível: ").append(veiculo.getCombustivel() != null ? veiculo.getCombustivel() : "N/A").append("\n");
        sb.append("Câmbio: ").append(veiculo.getCambio() != null ? veiculo.getCambio() : "N/A").append("\n");
        sb.append("Potência: ").append(veiculo.getPotencia() != null ? veiculo.getPotencia() + " CV" : "N/A").append("\n");
        sb.append("Cilindrada: ").append(veiculo.getCilindrada() != null ? veiculo.getCilindrada() + " cc" : "N/A").append("\n");
        sb.append("Peso: ").append(veiculo.getPeso() != null ? veiculo.getPeso() + " kg" : "N/A").append("\n");

        TextView textEspecificacoes = view.findViewById(R.id.text_especificacoes);
        if (textEspecificacoes != null) {
            textEspecificacoes.setText(sb.toString());
        }
    }
}