package br.jss.motoreviso.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import br.jss.motoreviso.R;
import br.jss.motoreviso.models.Piloto;

public class PilotoAdapter extends RecyclerView.Adapter<PilotoAdapter.ViewHolder> {

    public interface OnPilotoClickListener {
        void onPilotoClick(Piloto piloto);
    }

    private final List<Piloto> pilotos;
    private final OnPilotoClickListener listener;

    // Cor padrão usada quando avatarCor1 é inválido ou nulo
    private static final int COR_FALLBACK = 0xFF00A8FF; // secondary

    public PilotoAdapter(List<Piloto> pilotos, OnPilotoClickListener listener) {
        this.pilotos = pilotos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_piloto, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(pilotos.get(position));
    }

    @Override
    public int getItemCount() {
        return pilotos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final FrameLayout frameAvatar;
        private final TextView    tvNumero;
        private final ImageView   imgPreviewList;
        private final TextView    tvNome;
        private final TextView    tvApelido;
        private final TextView    tvCategoria;
        private final TextView    tvExperiencia;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            frameAvatar    = itemView.findViewById(R.id.frame_avatar);
            tvNumero       = itemView.findViewById(R.id.tv_numero_piloto);
            imgPreviewList = itemView.findViewById(R.id.img_avatar_preview_list);
            tvNome         = itemView.findViewById(R.id.tv_nome_piloto);
            tvApelido      = itemView.findViewById(R.id.tv_apelido_piloto);
            tvCategoria    = itemView.findViewById(R.id.tv_categoria_piloto);
            tvExperiencia  = itemView.findViewById(R.id.tv_experiencia_piloto);
        }

        void bind(Piloto piloto) {
            // Tentar exibir o preview do bitmap salvo
            boolean previewCarregado = carregarPreviewAvatar(piloto);

            if (!previewCarregado) {
                // Fallback: círculo colorido com número
                aplicarCorAvatar(piloto);
                imgPreviewList.setVisibility(View.GONE);
                tvNumero.setVisibility(View.VISIBLE);
            }

            // Número do piloto
            String numero = piloto.getNumeroPiloto();
            tvNumero.setText((numero != null && !numero.isEmpty()) ? numero : "#");

            // Cor do número (avatarCor2)
            try {
                if (piloto.getAvatarCor2() != null) {
                    tvNumero.setTextColor(Color.parseColor(piloto.getAvatarCor2()));
                } else {
                    tvNumero.setTextColor(Color.WHITE);
                }
            } catch (IllegalArgumentException ignored) {
                tvNumero.setTextColor(Color.WHITE);
            }

            // Nome completo
            String nome = piloto.getNomeCompleto();
            tvNome.setText((nome != null && !nome.isEmpty()) ? nome : "Piloto");

            // Apelido
            String apelido = piloto.getApelido();
            if (apelido != null && !apelido.trim().isEmpty()) {
                tvApelido.setText(apelido);
                tvApelido.setVisibility(View.VISIBLE);
            } else {
                tvApelido.setVisibility(View.GONE);
            }

            // Categoria
            String categoria = piloto.getCategoria();
            tvCategoria.setText((categoria != null && !categoria.isEmpty()) ? categoria : "Piloto");

            // Experiência
            Long anos = piloto.getAnosExperiencia();
            if (anos != null && anos > 0) {
                tvExperiencia.setText("• " + anos + (anos == 1 ? " ano" : " anos"));
                tvExperiencia.setVisibility(View.VISIBLE);
            } else {
                tvExperiencia.setVisibility(View.GONE);
            }

            // Clique no item
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPilotoClick(piloto);
            });
        }

        /** Tenta carregar o bitmap preview no ImageView da lista. Retorna true se carregou */
        private boolean carregarPreviewAvatar(Piloto piloto) {
            String path = piloto.getAvatarPreviewPath();
            if (path == null || path.isEmpty()) return false;

            File file = new File(path);
            if (!file.exists()) return false;

            try {
                Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bmp == null) return false;
                imgPreviewList.setImageBitmap(bmp);
                imgPreviewList.setVisibility(View.VISIBLE);
                tvNumero.setVisibility(View.GONE);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private void aplicarCorAvatar(Piloto piloto) {
            try {
                GradientDrawable drawable = (GradientDrawable) frameAvatar.getBackground().mutate();
                if (piloto.getAvatarCor1() != null) {
                    drawable.setColor(Color.parseColor(piloto.getAvatarCor1()));
                } else {
                    drawable.setColor(COR_FALLBACK);
                }
            } catch (IllegalArgumentException ignored) {
                // Cor inválida — usar fallback
                try {
                    GradientDrawable drawable = (GradientDrawable) frameAvatar.getBackground().mutate();
                    drawable.setColor(COR_FALLBACK);
                } catch (Exception ignored2) { /* nada */ }
            }
        }
    }
}
