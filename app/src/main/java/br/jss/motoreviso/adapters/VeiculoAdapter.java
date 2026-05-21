package br.jss.motoreviso.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import br.jss.motoreviso.R;
import br.jss.motoreviso.models.Veiculo;

public class VeiculoAdapter extends RecyclerView.Adapter<VeiculoAdapter.VeiculoViewHolder> {
    private List<Veiculo> veiculos;
    private OnVeiculoClickListener clickListener;

    public interface OnVeiculoClickListener {
        void onVeiculoClick(Veiculo veiculo);
    }

    public VeiculoAdapter(List<Veiculo> veiculos, OnVeiculoClickListener clickListener) {
        this.veiculos = veiculos;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public VeiculoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_veiculo, parent, false);
        return new VeiculoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VeiculoViewHolder holder, int position) {
        Veiculo veiculo = veiculos.get(position);
        holder.bind(veiculo);
    }

    @Override
    public int getItemCount() {
        return veiculos.size();
    }

    public class VeiculoViewHolder extends RecyclerView.ViewHolder {
        private ImageView imagemVeiculo;
        private TextView textMarcaModelo;
        private TextView textPlaca;
        private TextView textKmAtual;
        private TextView textProximaRevisao;
        private ProgressBar progressAlerta;

        public VeiculoViewHolder(@NonNull View itemView) {
            super(itemView);
            imagemVeiculo = itemView.findViewById(R.id.imagem_veiculo);
            textMarcaModelo = itemView.findViewById(R.id.text_marca_modelo);
            textPlaca = itemView.findViewById(R.id.text_placa);
            textKmAtual = itemView.findViewById(R.id.text_km_atual);
            textProximaRevisao = itemView.findViewById(R.id.text_proxima_revisao);
            progressAlerta = itemView.findViewById(R.id.progress_alerta);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onVeiculoClick(veiculos.get(position));
                }
            });
        }

        public void bind(Veiculo veiculo) {
            // Carrega imagem com Glide
            if (veiculo.getUrlImagemPrincipal() != null && !veiculo.getUrlImagemPrincipal().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(veiculo.getUrlImagemPrincipal())
                        .centerCrop()
                        .placeholder(R.drawable.ic_veiculo_placeholder)
                        .error(R.drawable.ic_veiculo_placeholder)
                        .into(imagemVeiculo);
            } else {
                imagemVeiculo.setImageResource(R.drawable.ic_veiculo_placeholder);
            }

            textMarcaModelo.setText(veiculo.getMarca() + " " + veiculo.getModelo());
            textPlaca.setText("Placa: " + veiculo.getPlaca());
            textKmAtual.setText(String.format("KM: %d", veiculo.getKmAtual() != null ? veiculo.getKmAtual() : 0));

            // Mostra se precisa de revisão urgente
            if (veiculo.precisaRevisao() != null && veiculo.precisaRevisao()) {
                progressAlerta.setVisibility(View.VISIBLE);
                Long kmFaltando = veiculo.getKmParaProximaRevisao();
                textProximaRevisao.setText("⚠️ Revisão em " + kmFaltando + " km");
                textProximaRevisao.setTextColor(itemView.getContext().getColor(R.color.red));
            } else {
                progressAlerta.setVisibility(View.GONE);
                Long kmFaltando = veiculo.getKmParaProximaRevisao();
                if (kmFaltando != null) {
                    textProximaRevisao.setText("Próxima revisão em " + kmFaltando + " km");
                } else {
                    textProximaRevisao.setText("Nenhuma revisão agendada");
                }
                textProximaRevisao.setTextColor(itemView.getContext().getColor(R.color.gray));
            }
        }
    }
}