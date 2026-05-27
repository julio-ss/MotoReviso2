package br.jss.motoreviso.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.Locale;

import br.jss.motoreviso.models.Trajeto;
import br.jss.motoreviso.models.Veiculo;

public class ShareHelper {

    private static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

    public static void compartilharWhatsApp(Context context, Veiculo veiculo, Trajeto trajeto) {
        String mensagem = gerarMensagemCompartilhamento(veiculo, trajeto);
        compartilharGenerico(context, "com.whatsapp", mensagem);
    }

    public static void compartilharInstagram(Context context, Veiculo veiculo, Trajeto trajeto) {
        String mensagem = gerarMensagemCompartilhamento(veiculo, trajeto);
        compartilharGenerico(context, "com.instagram.android", mensagem);
    }

    public static void compartilharEmail(Context context, Veiculo veiculo, Trajeto trajeto) {
        String mensagem = gerarMensagemCompartilhamento(veiculo, trajeto);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Informações do Trajeto - " + veiculo.getMarcaModelo());
        intent.putExtra(Intent.EXTRA_TEXT, mensagem);
        context.startActivity(Intent.createChooser(intent, "Enviar por email"));
    }

    public static void compartilharGenerico(Context context, String appPackage, String mensagem) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mensagem);

        try {
            intent.setPackage(appPackage);
            context.startActivity(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Intent.ACTION_SEND);
            fallback.setType("text/plain");
            fallback.putExtra(Intent.EXTRA_TEXT, mensagem);
            context.startActivity(Intent.createChooser(fallback, "Compartilhar com"));
        }
    }

    private static String gerarMensagemCompartilhamento(Veiculo veiculo, Trajeto trajeto) {
        StringBuilder sb = new StringBuilder();

        sb.append("🚗 *INFORMAÇÕES DO TRAJETO*\n\n");

        if (veiculo != null) {
            sb.append("📋 *Veículo*\n");
            sb.append("Marca/Modelo: ").append(veiculo.getMarcaModelo()).append("\n");
            sb.append("Placa: ").append(veiculo.getPlaca()).append("\n");
            sb.append("KM: ").append(String.format("%.0f km", veiculo.getKmAtual())).append("\n\n");
        }

        if (trajeto != null) {
            sb.append("🗺️ *Trajeto*\n");
            if (trajeto.getDataInicio() != null) {
                sb.append("Data: ").append(sdf.format(trajeto.getDataInicio())).append("\n");
            }
            if (trajeto.getOrigem() != null) {
                sb.append("De: ").append(trajeto.getOrigem()).append("\n");
            }
            if (trajeto.getDestino() != null) {
                sb.append("Para: ").append(trajeto.getDestino()).append("\n");
            }
            sb.append("Distância: ").append(String.format("%.2f km", trajeto.getKmRodados())).append("\n");
            if (trajeto.getDuracao() != null) {
                long horas = trajeto.getDuracao() / 60;
                long min = trajeto.getDuracao() % 60;
                sb.append("Duração: ");
                if (horas > 0) {
                    sb.append(horas).append("h ");
                }
                sb.append(min).append("m\n");
            }
            sb.append("Velocidade Máxima: ").append(String.format("%.1f km/h", trajeto.getVelocidadeMaxima())).append("\n");
        }

        sb.append("\n_Compartilhado via MotoReviso_");

        return sb.toString();
    }
}
