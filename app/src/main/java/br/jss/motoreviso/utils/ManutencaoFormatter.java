package br.jss.motoreviso.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ManutencaoFormatter {
    private static final SimpleDateFormat sdfData = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
    private static final SimpleDateFormat sdfCompleta = new SimpleDateFormat("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));
    private static final SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm", new Locale("pt", "BR"));
    private static final SimpleDateFormat sdfDataHora = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));

    /**
     * Formata timestamp para data no formato dd/MM/yyyy
     */
    public static String formatarData(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            return "-";
        }
        try {
            return sdfData.format(new Date(timestamp));
        } catch (Exception e) {
            return "-";
        }
    }

    /**
     * Formata timestamp para data por extenso (dd de MMMM de yyyy)
     */
    public static String formatarDataCompleta(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            return "-";
        }
        try {
            return sdfCompleta.format(new Date(timestamp));
        } catch (Exception e) {
            return "-";
        }
    }

    /**
     * Formata timestamp para data e hora (dd/MM/yyyy HH:mm)
     */
    public static String formatarDataHora(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            return "-";
        }
        try {
            return sdfDataHora.format(new Date(timestamp));
        } catch (Exception e) {
            return "-";
        }
    }

    /**
     * Formata timestamp para hora (HH:mm)
     */
    public static String formatarHora(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            return "-";
        }
        try {
            return sdfHora.format(new Date(timestamp));
        } catch (Exception e) {
            return "-";
        }
    }

    /**
     * Formata valor em reais (R$ 0,00)
     */
    public static String formatarCusto(Double valor) {
        if (valor == null || valor <= 0) {
            return "R$ 0,00";
        }
        try {
            return String.format(new Locale("pt", "BR"), "R$ %.2f", valor);
        } catch (Exception e) {
            return "R$ 0,00";
        }
    }

    /**
     * Formata valor em reais sem simbolo (0,00)
     */
    public static String formatarValor(Double valor) {
        if (valor == null || valor <= 0) {
            return "0,00";
        }
        try {
            return String.format(new Locale("pt", "BR"), "%.2f", valor);
        } catch (Exception e) {
            return "0,00";
        }
    }

    /**
     * Formata KM (número km)
     */
    public static String formatarKm(Long km) {
        if (km == null || km <= 0) {
            return "0 km";
        }
        try {
            return String.format("%d km", km);
        } catch (Exception e) {
            return "0 km";
        }
    }

    /**
     * Formata KM sem unidade
     */
    public static String formatarKmSemUnidade(Long km) {
        if (km == null || km <= 0) {
            return "0";
        }
        try {
            return String.format("%d", km);
        } catch (Exception e) {
            return "0";
        }
    }

    /**
     * Formata duração em milissegundos para texto legível
     */
    public static String formatarTempo(Long duracaoMs) {
        if (duracaoMs == null || duracaoMs <= 0) {
            return "0 minutos";
        }

        try {
            long segundos = duracaoMs / 1000;
            long minutos = segundos / 60;
            long horas = minutos / 60;
            long dias = horas / 24;

            if (dias > 0) {
                return dias == 1 ? dias + " dia" : dias + " dias";
            } else if (horas > 0) {
                return horas == 1 ? horas + " hora" : horas + " horas";
            } else if (minutos > 0) {
                return minutos == 1 ? minutos + " minuto" : minutos + " minutos";
            } else {
                return segundos == 1 ? segundos + " segundo" : segundos + " segundos";
            }
        } catch (Exception e) {
            return "0 minutos";
        }
    }

    /**
     * Formata duração em minutos para texto legível
     */
    public static String formatarTempoMinutos(Long minutos) {
        if (minutos == null || minutos <= 0) {
            return "0 minutos";
        }

        try {
            long horas = minutos / 60;
            long mins = minutos % 60;
            long dias = horas / 24;
            long hrs = horas % 24;

            if (dias > 0) {
                return String.format("%d dia%s", dias, dias == 1 ? "" : "s");
            } else if (hrs > 0) {
                return String.format("%dh %dmin", hrs, mins);
            } else {
                return String.format("%d minuto%s", mins, mins == 1 ? "" : "s");
            }
        } catch (Exception e) {
            return "0 minutos";
        }
    }

    /**
     * Formata lista de peças em string separada por vírgula
     */
    public static String formatarPecas(List<String> pecas) {
        if (pecas == null || pecas.isEmpty()) {
            return "-";
        }

        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pecas.size(); i++) {
                String peca = pecas.get(i);
                if (peca != null && !peca.trim().isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(peca.trim());
                }
            }
            return sb.length() > 0 ? sb.toString() : "-";
        } catch (Exception e) {
            return "-";
        }
    }

    /**
     * Formata lista de peças em bullet points para exibição
     */
    public static String formatarPecasComPrefixo(List<String> pecas) {
        if (pecas == null || pecas.isEmpty()) {
            return "Nenhuma peça registrada";
        }

        try {
            StringBuilder sb = new StringBuilder();
            for (String peca : pecas) {
                if (peca != null && !peca.trim().isEmpty()) {
                    sb.append("• ").append(peca.trim()).append("\n");
                }
            }
            return sb.length() > 0 ? sb.toString().trim() : "Nenhuma peça registrada";
        } catch (Exception e) {
            return "Nenhuma peça registrada";
        }
    }

    /**
     * Verifica se a revisão está urgente (próxima em até 500 km)
     */
    public static boolean ehRevisaoUrgente(Long proximaRevisaoKm, Long kmAtual) {
        if (proximaRevisaoKm == null || kmAtual == null) {
            return false;
        }
        try {
            long diferenca = proximaRevisaoKm - kmAtual;
            return diferenca <= 500 && diferenca > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica se a revisão está vencida
     */
    public static boolean ehRevisaoVencida(Long proximaRevisaoKm, Long kmAtual) {
        if (proximaRevisaoKm == null || kmAtual == null) {
            return false;
        }
        try {
            return kmAtual >= proximaRevisaoKm;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifica se a revisão está próxima (próxima em até 1000 km)
     */
    public static boolean ehRevisaoProxima(Long proximaRevisaoKm, Long kmAtual) {
        if (proximaRevisaoKm == null || kmAtual == null) {
            return false;
        }
        try {
            long diferenca = proximaRevisaoKm - kmAtual;
            return diferenca <= 1000 && diferenca > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Retorna status da revisão (OK, URGENTE, VENCIDA)
     */
    public static String getStatusRevisao(Long proximaRevisaoKm, Long kmAtual) {
        if (proximaRevisaoKm == null || kmAtual == null) {
            return "OK";
        }

        try {
            if (ehRevisaoVencida(proximaRevisaoKm, kmAtual)) {
                return "VENCIDA";
            } else if (ehRevisaoUrgente(proximaRevisaoKm, kmAtual)) {
                return "URGENTE";
            }
            return "OK";
        } catch (Exception e) {
            return "OK";
        }
    }

    /**
     * Retorna cor baseada no status da revisão
     */
    public static int getCorStatusRevisao(Long proximaRevisaoKm, Long kmAtual) {
        String status = getStatusRevisao(proximaRevisaoKm, kmAtual);

        switch (status) {
            case "VENCIDA":
            case "URGENTE":
                return android.graphics.Color.RED;
            default:
                return android.graphics.Color.GREEN;
        }
    }

    /**
     * Calcula dias até próxima revisão
     */
    public static String calcularDiasAteRevisao(Long proximaRevisaoData) {
        if (proximaRevisaoData == null || proximaRevisaoData == 0) {
            return "-";
        }

        try {
            long agora = System.currentTimeMillis();
            long diferenca = proximaRevisaoData - agora;

            if (diferenca < 0) {
                return "Vencida";
            }

            long dias = diferenca / (1000 * 60 * 60 * 24);

            if (dias == 0) {
                return "Hoje";
            } else if (dias == 1) {
                return "1 dia";
            } else if (dias > 1 && dias <= 7) {
                return dias + " dias";
            } else if (dias <= 30) {
                long semanas = dias / 7;
                return semanas + " semana" + (semanas == 1 ? "" : "s");
            } else {
                long meses = dias / 30;
                return meses + " mês" + (meses == 1 ? "" : "es");
            }
        } catch (Exception e) {
            return "-";
        }
    }

    /**
     * Calcula KM até próxima revisão
     */
    public static String calcularKmAteRevisao(Long proximaRevisaoKm, Long kmAtual) {
        if (proximaRevisaoKm == null || kmAtual == null) {
            return "-";
        }

        try {
            long diferenca = proximaRevisaoKm - kmAtual;

            if (diferenca < 0) {
                return "Vencida";
            } else if (diferenca == 0) {
                return "Hoje";
            } else if (diferenca <= 500) {
                return "Urgente: " + diferenca + " km";
            } else {
                return diferenca + " km";
            }
        } catch (Exception e) {
            return "-";
        }
    }

    /**
     * Formata tipo de manutenção para exibição
     */
    public static String formatarTipoManutencao(String tipo) {
        if (tipo == null || tipo.isEmpty()) {
            return "Revisão";
        }

        return tipo.substring(0, 1).toUpperCase() + tipo.substring(1).toLowerCase();
    }

    /**
     * Formata descrição para exibição (limita tamanho)
     */
    public static String formatarDescricao(String descricao, int maxCaracteres) {
        if (descricao == null || descricao.isEmpty()) {
            return "-";
        }

        try {
            descricao = descricao.trim();
            if (descricao.length() > maxCaracteres) {
                return descricao.substring(0, maxCaracteres) + "...";
            }
            return descricao;
        } catch (Exception e) {
            return "-";
        }
    }

    /**
     * Valida se os dados obrigatórios foram preenchidos
     */
    public static boolean validarDadosObrigatorios(Long dataRevisao, Long kmRevisao, Double custo) {
        return dataRevisao != null && dataRevisao > 0 &&
                kmRevisao != null && kmRevisao > 0 &&
                custo != null && custo > 0;
    }

    /**
     * Calcula média de custo por manutenção
     */
    public static Double calcularMediaCusto(List<Long> custos) {
        if (custos == null || custos.isEmpty()) {
            return 0.0;
        }

        try {
            long total = 0;
            for (Long custo : custos) {
                if (custo != null && custo > 0) {
                    total += custo;
                }
            }
            return (double) total / custos.size();
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Calcula custo total de lista de manutenções
     */
    public static Double calcularCustoTotal(List<Long> custos) {
        if (custos == null || custos.isEmpty()) {
            return 0.0;
        }

        try {
            long total = 0;
            for (Long custo : custos) {
                if (custo != null && custo > 0) {
                    total += custo;
                }
            }
            return (double) total;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Retorna mensagem legível sobre status da manutenção
     */
    public static String getMensagemStatus(Long proximaRevisaoKm, Long kmAtual, Long proximaRevisaoData) {
        String statusKm = getStatusRevisao(proximaRevisaoKm, kmAtual);

        switch (statusKm) {
            case "VENCIDA":
                return "⚠️ Revisão vencida! Agende imediatamente.";
            case "URGENTE":
                long kmFaltando = proximaRevisaoKm - kmAtual;
                return "⚠️ Revisão urgente! Faltam " + kmFaltando + " km";
            default:
                return "✓ Manutenção em dia";
        }
    }

    /**
     * Compara duas datas e retorna a diferença em dias
     */
    public static Long diferencaDias(Long data1, Long data2) {
        if (data1 == null || data2 == null) {
            return 0L;
        }

        try {
            long diferenca = Math.abs(data1 - data2);
            return diferenca / (1000 * 60 * 60 * 24);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Gera relatório resumido da manutenção
     */
    public static String gerarRelatorioResumo(
            String tipo,
            Long dataRevisao,
            Long kmRevisao,
            Double custo,
            String mecanico,
            List<String> pecas) {

        StringBuilder sb = new StringBuilder();
        sb.append("📋 RESUMO DA MANUTENÇÃO\n\n");
        sb.append("Tipo: ").append(formatarTipoManutencao(tipo)).append("\n");
        sb.append("Data: ").append(formatarDataCompleta(dataRevisao)).append("\n");
        sb.append("KM: ").append(formatarKm(kmRevisao)).append("\n");
        sb.append("Custo: ").append(formatarCusto(custo)).append("\n");
        sb.append("Mecânico: ").append(mecanico != null && !mecanico.isEmpty() ? mecanico : "-").append("\n");
        sb.append("Peças: ").append(formatarPecas(pecas)).append("\n");

        return sb.toString();
    }
}