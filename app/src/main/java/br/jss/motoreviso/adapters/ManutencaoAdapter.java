package br.jss.motoreviso.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.models.Manutencao;

public class ManutencaoAdapter extends RecyclerView.Adapter<ManutencaoAdapter.ManutencaoViewHolder> {
    private List<Manutencao> manutencoes;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));

    public ManutencaoAdapter(List<Manutencao> manutencoes) {
        this.manutencoes = manutencoes;
    }

    @NonNull
    @Override
    public ManutencaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manutencao, parent, false);
        return new ManutencaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManutencaoViewHolder holder, int position) {
        Manutencao manutencao = manutencoes.get(position);
        holder.bind(manutencao);
    }

    @Override
    public int getItemCount() {
        return manutencoes.size();
    }

    public class ManutencaoViewHolder extends RecyclerView.ViewHolder {
        private TextView textData;
        private TextView textKm;
        private TextView textTipo;
        private TextView textCusto;
        private TextView textPecas;

        public ManutencaoViewHolder(@NonNull View itemView) {
            super(itemView);
            textData = itemView.findViewById(R.id.text_data);
            textKm = itemView.findViewById(R.id.text_km);
            textTipo = itemView.findViewById(R.id.text_tipo);
            textCusto = itemView.findViewById(R.id.text_custo);
            textPecas = itemView.findViewById(R.id.text_pecas);
        }

        public void bind(Manutencao manutencao) {
            if (manutencao.getDataRevisao() != null) {
                textData.setText(sdf.format(manutencao.getDataRevisao()));
            }
            textKm.setText(String.format("KM: %d", manutencao.getKmRevisao() != null ? manutencao.getKmRevisao() : 0));
            textTipo.setText(manutencao.getTipo() != null ? manutencao.getTipo() : "Revisão");

            if (manutencao.getCusto() != null) {
                textCusto.setText(String.format("R$ %.2f", manutencao.getCusto()));
            }

            if (manutencao.getPecasTrocadas() != null && !manutencao.getPecasTrocadas().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String peca : manutencao.getPecasTrocadas()) {
                    sb.append(peca).append(", ");
                }
                String pecas = sb.toString();
                if (pecas.length() > 2) {
                    pecas = pecas.substring(0, pecas.length() - 2);
                }
                textPecas.setText("Peças: " + pecas);
            }
        }
    }
}