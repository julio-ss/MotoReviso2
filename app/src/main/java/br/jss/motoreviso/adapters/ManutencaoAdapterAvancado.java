package br.jss.motoreviso.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import br.jss.motoreviso.R;
import br.jss.motoreviso.models.Manutencao;

public class ManutencaoAdapterAvancado extends RecyclerView.Adapter<ManutencaoAdapterAvancado.ManutencaoViewHolder> {
    private List<Manutencao> manutencoes;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
    private OnManutencaoClickListener listener;

    public interface OnManutencaoClickListener {
        void onManutencaoClick(Manutencao manutencao);
        void onEditClick(Manutencao manutencao);
        void onDeleteClick(Manutencao manutencao);
    }

    public ManutencaoAdapterAvancado(List<Manutencao> manutencoes) {
        this.manutencoes = manutencoes;
        this.listener = null;
    }

    public ManutencaoAdapterAvancado(List<Manutencao> manutencoes, OnManutencaoClickListener listener) {
        this.manutencoes = manutencoes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ManutencaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manutencao_avancado, parent, false);
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
        private TextView textMecanico;
        private ImageButton btnMenu;

        public ManutencaoViewHolder(@NonNull View itemView) {
            super(itemView);
            textData = itemView.findViewById(R.id.text_data);
            textKm = itemView.findViewById(R.id.text_km);
            textTipo = itemView.findViewById(R.id.text_tipo);
            textCusto = itemView.findViewById(R.id.text_custo);
            textPecas = itemView.findViewById(R.id.text_pecas);
            textMecanico = itemView.findViewById(R.id.text_mecanico);
            btnMenu = itemView.findViewById(R.id.btn_menu);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onManutencaoClick(manutencoes.get(position));
                }
            });

            btnMenu.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    mostrarMenu(v, position);
                }
            });
        }

        public void bind(Manutencao manutencao) {
            if (manutencao.getDataRevisao() != null) {
                textData.setText(sdf.format(manutencao.getDataRevisao()));
            } else {
                textData.setText("-");
            }

            Long km = manutencao.getKmRevisao();
            if (km != null && km > 0) {
                textKm.setText(String.format("KM: %d", km));
            } else {
                textKm.setText("KM: -");
            }

            String tipo = manutencao.getTipo();
            if (tipo != null && !tipo.isEmpty()) {
                textTipo.setText(tipo);
            } else {
                textTipo.setText("Revisão");
            }

            Double custo = manutencao.getCusto();
            if (custo != null && custo > 0) {
                textCusto.setText(String.format("R$ %.2f", custo));
            } else {
                textCusto.setText("R$ 0.00");
            }

            String mecanico = manutencao.getMecanico();
            if (mecanico != null && !mecanico.isEmpty()) {
                textMecanico.setText(mecanico);
                textMecanico.setVisibility(View.VISIBLE);
            } else {
                textMecanico.setVisibility(View.GONE);
            }

            if (manutencao.getPecasTrocadas() != null && !manutencao.getPecasTrocadas().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < manutencao.getPecasTrocadas().size(); i++) {
                    sb.append(manutencao.getPecasTrocadas().get(i));
                    if (i < manutencao.getPecasTrocadas().size() - 1) {
                        sb.append(", ");
                    }
                }
                textPecas.setText("Peças: " + sb.toString());
                textPecas.setVisibility(View.VISIBLE);
            } else {
                textPecas.setVisibility(View.GONE);
            }
        }

        private void mostrarMenu(View view, int position) {
            PopupMenu popup = new PopupMenu(itemView.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.menu_manutencao, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                Manutencao manutencao = manutencoes.get(position);
                if (item.getItemId() == R.id.menu_editar) {
                    if (listener != null) {
                        listener.onEditClick(manutencao);
                    }
                } else if (item.getItemId() == R.id.menu_deletar) {
                    if (listener != null) {
                        listener.onDeleteClick(manutencao);
                    }
                }
                return true;
            });
            popup.show();
        }
    }
}