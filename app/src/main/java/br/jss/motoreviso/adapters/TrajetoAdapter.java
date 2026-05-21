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
import br.jss.motoreviso.models.Trajeto;

public class TrajetoAdapter extends RecyclerView.Adapter<TrajetoAdapter.TrajetoViewHolder> {
    private List<Trajeto> trajetos;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

    public TrajetoAdapter(List<Trajeto> trajetos) {
        this.trajetos = trajetos;
    }

    @NonNull
    @Override
    public TrajetoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trajeto, parent, false);
        return new TrajetoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrajetoViewHolder holder, int position) {
        Trajeto trajeto = trajetos.get(position);
        holder.bind(trajeto);
    }

    @Override
    public int getItemCount() {
        return trajetos.size();
    }

    public class TrajetoViewHolder extends RecyclerView.ViewHolder {
        private TextView textData;
        private TextView textOrigem;
        private TextView textDestino;
        private TextView textKmRodados;
        private TextView textVelocidadeMax;
        private TextView textDuracao;

        public TrajetoViewHolder(@NonNull View itemView) {
            super(itemView);
            textData = itemView.findViewById(R.id.text_data);
            textOrigem = itemView.findViewById(R.id.text_origem);
            textDestino = itemView.findViewById(R.id.text_destino);
            textKmRodados = itemView.findViewById(R.id.text_km_rodados);
            textVelocidadeMax = itemView.findViewById(R.id.text_velocidade_max);
            textDuracao = itemView.findViewById(R.id.text_duracao);
        }

        public void bind(Trajeto trajeto) {
            if (trajeto.getDataInicio() != null) {
                textData.setText(sdf.format(trajeto.getDataInicio()));
            }

            textOrigem.setText(trajeto.getOrigem() != null ? trajeto.getOrigem() : "Desconhecido");
            textDestino.setText(trajeto.getDestino() != null ? trajeto.getDestino() : "Desconhecido");

            textKmRodados.setText(String.format("%.2f km",
                    trajeto.getKmRodados() != null ? trajeto.getKmRodados() : 0));

            textVelocidadeMax.setText(String.format("Máx: %.1f km/h",
                    trajeto.getVelocidadeMaxima() != null ? trajeto.getVelocidadeMaxima() : 0));

            if (trajeto.getDuracao() != null) {
                long horas = trajeto.getDuracao() / 60;
                long minutos = trajeto.getDuracao() % 60;
                textDuracao.setText(String.format("%02d:%02d", horas, minutos));
            }
        }
    }
}