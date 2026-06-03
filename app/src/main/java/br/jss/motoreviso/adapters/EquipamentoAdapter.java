package br.jss.motoreviso.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.models.Equipamento;

public class EquipamentoAdapter extends RecyclerView.Adapter<EquipamentoAdapter.ViewHolder> {

    public interface OnEquipamentoClickListener {
        void onEquipamentoClick(Equipamento equipamento);
    }

    private final List<Equipamento> equipamentos;
    private final OnEquipamentoClickListener listener;

    public EquipamentoAdapter(List<Equipamento> equipamentos, OnEquipamentoClickListener listener) {
        this.equipamentos = equipamentos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipamento, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(equipamentos.get(position));
    }

    @Override
    public int getItemCount() {
        return equipamentos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivTipoIcone;
        private final TextView tvTipo;
        private final TextView tvMarcaModelo;
        private final TextView tvValidade;
        private final TextView tvStatusValidade;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivTipoIcone      = itemView.findViewById(R.id.iv_tipo_icone);
            tvTipo           = itemView.findViewById(R.id.tv_tipo_equipamento);
            tvMarcaModelo    = itemView.findViewById(R.id.tv_marca_modelo_equipamento);
            tvValidade       = itemView.findViewById(R.id.tv_validade_equipamento);
            tvStatusValidade = itemView.findViewById(R.id.tv_status_validade);
        }

        void bind(Equipamento equipamento) {
            // Tipo
            String tipo = equipamento.getTipo();
            tvTipo.setText((tipo != null && !tipo.isEmpty()) ? tipo : "Equipamento");

            // Marca + Modelo
            String marca = equipamento.getMarca();
            String modelo = equipamento.getModelo();
            if ((marca != null && !marca.isEmpty()) || (modelo != null && !modelo.isEmpty())) {
                StringBuilder sb = new StringBuilder();
                if (marca != null && !marca.isEmpty()) sb.append(marca);
                if (modelo != null && !modelo.isEmpty()) {
                    if (sb.length() > 0) sb.append(" · ");
                    sb.append(modelo);
                }
                tvMarcaModelo.setText(sb.toString());
                tvMarcaModelo.setVisibility(View.VISIBLE);
            } else {
                tvMarcaModelo.setVisibility(View.GONE);
            }

            // Data de validade
            if (equipamento.getDataValidade() != null) {
                String dataStr = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"))
                        .format(new Date(equipamento.getDataValidade()));
                tvValidade.setText("Validade: " + dataStr);
                tvValidade.setVisibility(View.VISIBLE);
            } else {
                tvValidade.setText("Sem data de validade");
                tvValidade.setVisibility(View.VISIBLE);
            }

            // Badge de status de validade
            int status = equipamento.getStatusValidade();
            int corBadge;
            switch (status) {
                case 0: corBadge = ContextCompat.getColor(itemView.getContext(), R.color.success); break;
                case 1: corBadge = ContextCompat.getColor(itemView.getContext(), R.color.warning); break;
                default: corBadge = ContextCompat.getColor(itemView.getContext(), R.color.error); break;
            }
            tvStatusValidade.setTextColor(corBadge);

            // Clique
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEquipamentoClick(equipamento);
            });
        }
    }
}
