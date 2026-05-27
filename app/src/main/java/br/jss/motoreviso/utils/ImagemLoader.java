package br.jss.motoreviso.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import br.jss.motoreviso.R;

public class ImagemLoader {
    /**
     * Carrega uma imagem local com cache
     * Se a imagem não existir ou o URI for inválido, mostra a imagem padrão
     */
    public static void carregarImagem(Context context, ImageView imageView, String imagemUri) {
        if (context == null || imageView == null) {
            return;
        }

        if (imagemUri == null || imagemUri.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_car_modern);
            return;
        }

        try {
            Uri uri = Uri.parse(imagemUri);
            Glide.with(context)
                    .load(uri)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .placeholder(R.drawable.ic_car_modern)
                    .error(R.drawable.ic_car_modern)
                    .into(imageView);
        } catch (Exception e) {
            // Se houver erro ao carregar, mostrar imagem padrão
            imageView.setImageResource(R.drawable.ic_car_modern);
        }
    }

    /**
     * Carrega uma imagem com tamanho e fallback
     */
    public static void carregarImagemComFallback(Context context, ImageView imageView, String imagemUri, int fallbackDrawable) {
        if (context == null || imageView == null) {
            return;
        }

        if (imagemUri == null || imagemUri.isEmpty()) {
            imageView.setImageResource(fallbackDrawable);
            return;
        }

        try {
            Uri uri = Uri.parse(imagemUri);
            Glide.with(context)
                    .load(uri)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .placeholder(fallbackDrawable)
                    .error(fallbackDrawable)
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageResource(fallbackDrawable);
        }
    }
}
