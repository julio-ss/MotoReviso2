package br.jss.motoreviso.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;

import com.bumptech.glide.Glide;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.models.Veiculo;

public class VeiculoAdapter extends RecyclerView.Adapter<VeiculoAdapter.VeiculoViewHolder> {
    private List<Veiculo> veiculos;
    private OnVeiculoClickListener clickListener;
    private OnPrincipalChangedListener principalListener;

    public interface OnVeiculoClickListener {
        void onVeiculoClick(Veiculo veiculo);
    }

    public interface OnPrincipalChangedListener {
        void onPrincipalChanged(Veiculo veiculo);
    }

    public VeiculoAdapter(List<Veiculo> veiculos, OnVeiculoClickListener clickListener) {
        this.veiculos = veiculos;
        this.clickListener = clickListener;
    }

    public void setPrincipalListener(OnPrincipalChangedListener listener) {
        this.principalListener = listener;
    }

    @NonNull
    @Override
    public VeiculoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_veiculo_garagem, parent, false);
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
        private TextView textMarca;
        private TextView textModelo;
        private TextView textQuilometragem;
        private TextView textProximaRevisao;
        private LinearLayout statusBadge;
        private TextView textStatus;
        private Button btnPrincipal;

        public VeiculoViewHolder(@NonNull View itemView) {
            super(itemView);
            imagemVeiculo = itemView.findViewById(R.id.img_veiculo);
            textMarca = itemView.findViewById(R.id.text_marca);
            textModelo = itemView.findViewById(R.id.text_modelo);
            textQuilometragem = itemView.findViewById(R.id.text_quilometragem);
            textProximaRevisao = itemView.findViewById(R.id.text_proxima_revisao);
            statusBadge = itemView.findViewById(R.id.status_badge);
            textStatus = itemView.findViewById(R.id.text_status);
            btnPrincipal = itemView.findViewById(R.id.btn_principal);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onVeiculoClick(veiculos.get(position));
                }
            });

            btnPrincipal.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && principalListener != null) {
                    principalListener.onPrincipalChanged(veiculos.get(position));
                }
            });
        }

        public void bind(Veiculo veiculo) {
            // Carrega imagem com Glide
            String imagemPath = veiculo.getUrlImagemPrincipal();
            if (imagemPath != null && !imagemPath.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(Uri.parse(imagemPath))
                        .centerCrop()
                        .placeholder(R.drawable.ic_veiculo_placeholder)
                        .error(R.drawable.ic_veiculo_placeholder)
                        .into(imagemVeiculo);
            } else {
                imagemVeiculo.setImageResource(R.drawable.ic_veiculo_placeholder);
            }

            // Marca e Modelo
            textMarca.setText(veiculo.getMarca() != null ? veiculo.getMarca() : "");
            textModelo.setText(veiculo.getModelo() != null ? veiculo.getModelo() : "");

            // Quilometragem
            Long kmAtual = veiculo.getKmAtual();
            if (kmAtual != null) {
                textQuilometragem.setText(String.format(Locale.US, "%,d km", kmAtual).replace(",", "."));
            } else {
                textQuilometragem.setText("0 km");
            }

            // Próxima revisão
            if (veiculo.getDataProximaRevisao() != null && veiculo.getDataProximaRevisao() > 0) {
                try {
                    Date date = new Date(veiculo.getDataProximaRevisao());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM", new Locale("pt", "BR"));
                    textProximaRevisao.setText(outputFormat.format(date));
                } catch (Exception e) {
                    textProximaRevisao.setText("Data a agendar");
                }
            } else {
                textProximaRevisao.setText("Data a agendar");
            }

            // Status
            updateStatusBadge(veiculo);

            // Botão Principal
            if (veiculo.isPrincipal()) {
                btnPrincipal.setText("✓ É o principal");
                btnPrincipal.setEnabled(false);
                btnPrincipal.setAlpha(0.6f);
            } else {
                btnPrincipal.setText("Definir como principal");
                btnPrincipal.setEnabled(true);
                btnPrincipal.setAlpha(1.0f);
            }
        }

        private void updateStatusBadge(Veiculo veiculo) {
            String status;
            int statusColor;

            if (veiculo.precisaRevisao() != null && veiculo.precisaRevisao()) {
                status = "Revisão vencida";
                statusColor = itemView.getContext().getColor(R.color.red);
            } else {
                status = "Em dia";
                statusColor = itemView.getContext().getColor(R.color.green);
            }

            textStatus.setText(status);
            ViewCompat.setBackgroundTintList(statusBadge, ColorStateList.valueOf(statusColor));
        }
    }
}