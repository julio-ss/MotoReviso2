package br.jss.motoreviso.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import br.jss.motoreviso.R;
import br.jss.motoreviso.models.ImagemVeiculo;

import java.util.List;

public class ImagemVeiculoAdapter extends RecyclerView.Adapter<ImagemVeiculoAdapter.ImagemViewHolder> {

    private final List<ImagemVeiculo> imagens;
    private OnImagemClickListener listener;

    public interface OnImagemClickListener {
        void onImagemClick(ImagemVeiculo imagem, int position);
    }

    public ImagemVeiculoAdapter(List<ImagemVeiculo> imagens) {
        this.imagens = imagens;
    }

    public ImagemVeiculoAdapter(List<ImagemVeiculo> imagens, OnImagemClickListener listener) {
        this.imagens = imagens;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImagemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_imagem, parent, false);
        return new ImagemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImagemViewHolder holder, int position) {
        ImagemVeiculo imagem = imagens.get(position);
        holder.bind(imagem, listener);
    }

    @Override
    public int getItemCount() {
        return imagens.size();
    }

    static class ImagemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgItem;

        ImagemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgItem = itemView.findViewById(R.id.img_item);
        }

        void bind(ImagemVeiculo imagem, OnImagemClickListener listener) {
            if (imagem.getUrlImagem() != null && !imagem.getUrlImagem().isEmpty()) {
                Glide.with(imgItem.getContext())
                        .load(imagem.getUrlImagem())
                        .centerCrop()
                        .placeholder(R.drawable.ic_image_modern)
                        .error(R.drawable.ic_image_modern)
                        .into(imgItem);
            } else {
                imgItem.setImageResource(R.drawable.ic_image_modern);
            }

            if (listener != null) {
                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_ID) {
                        listener.onImagemClick(imagem, pos);
                    }
                });
            }
        }
    }
}
