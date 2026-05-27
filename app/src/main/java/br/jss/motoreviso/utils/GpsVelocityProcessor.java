package br.jss.motoreviso.utils;

import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe para processar e filtrar leituras de velocidade do GPS
 * Implementa múltiplas técnicas para melhorar precisão
 */
public class GpsVelocityProcessor {
    private static final String TAG = "GpsVelocityProcessor";

    // Configurações de filtro
    private static final double MIN_VELOCITY = 0.5; // Ignorar velocidades < 0.5 km/h
    private static final double MAX_VELOCITY = 350; // Ignorar velocidades > 350 km/h (limite carros)
    private static final int BUFFER_SIZE = 5; // Manter histórico de 5 leituras
    private static final double OUTLIER_THRESHOLD = 2.5; // Desvios maiores que 2.5x são outliers

    private final List<Double> velocidadeBuffer;
    private double velocidadeMaxima = 0;
    private double velocidadeMediaMovel = 0;
    private Long ultimoTempo = null;
    private Location ultimaLocalizacao = null;

    public GpsVelocityProcessor() {
        this.velocidadeBuffer = new ArrayList<>();
    }

    /**
     * Processa uma nova leitura de localização e retorna a velocidade filtrada
     *
     * @param location A nova localização do GPS
     * @return Velocidade filtrada em km/h
     */
    public synchronized double procesarLocalizacao(Location location) {
        if (location == null || !location.hasSpeed()) {
            Log.w(TAG, "Localização inválida ou sem velocidade");
            return velocidadeMediaMovel;
        }

        // Converter m/s para km/h (1 m/s = 3.6 km/h)
        double velocidadeKmh = location.getSpeed() * 3.6;

        // Aplicar primeiro filtro: remover valores claramente inválidos
        velocidadeKmh = filtrarValoresInvalidos(velocidadeKmh);

        // Aplicar filtro de outliers
        velocidadeKmh = filtrarOutliers(velocidadeKmh);

        // Validação adicional: comparar com movimento real
        if (ultimaLocalizacao != null && ultimoTempo != null) {
            double velocidadeCalculada = calcularVelocidadePorDistancia(location);
            velocidadeKmh = validarComDistancia(velocidadeKmh, velocidadeCalculada);
        }

        ultimaLocalizacao = location;
        ultimoTempo = System.currentTimeMillis();

        // Adicionar ao buffer de histórico
        velocidadeBuffer.add(velocidadeKmh);
        if (velocidadeBuffer.size() > BUFFER_SIZE) {
            velocidadeBuffer.remove(0);
        }

        // Calcular média móvel
        velocidadeMediaMovel = calcularMediaMovel();

        // Atualizar velocidade máxima com valor mais confiável
        if (velocidadeMediaMovel > velocidadeMaxima) {
            velocidadeMaxima = velocidadeMediaMovel;
            Log.d(TAG, String.format("Nova velocidade máxima: %.1f km/h (Raw GPS: %.1f)",
                    velocidadeMaxima, velocidadeKmh));
        }

        return velocidadeMediaMovel;
    }

    /**
     * Remove valores claramente inválidos
     */
    private double filtrarValoresInvalidos(double velocidade) {
        // Se é zero ou muito pequeno, considerar como parado
        if (velocidade < MIN_VELOCITY) {
            return 0.0;
        }

        // Se é maior que o limite físico, é erro do GPS
        if (velocidade > MAX_VELOCITY) {
            Log.w(TAG, String.format("Velocidade impossível filtrada: %.1f km/h", velocidade));
            return velocidadeMediaMovel; // Retorna última velocidade válida
        }

        return velocidade;
    }

    /**
     * Remove outliers usando desvio padrão
     */
    private double filtrarOutliers(double velocidade) {
        if (velocidadeBuffer.isEmpty()) {
            return velocidade;
        }

        double media = calcularMedia();
        double desvio = calcularDesviopadrao(media);

        // Se o desvio padrão é muito pequeno (velocidade estável), desvio é muito grande
        if (desvio > 0 && Math.abs(velocidade - media) > desvio * OUTLIER_THRESHOLD) {
            Log.d(TAG, String.format("Outlier filtrado: %.1f km/h (média: %.1f, desvio: %.1f)",
                    velocidade, media, desvio));
            return media; // Retorna a média em vez do outlier
        }

        return velocidade;
    }

    /**
     * Valida a velocidade do GPS com a velocidade calculada por distância
     */
    private double validarComDistancia(double velocidadeGps, double velocidadeCalculada) {
        if (velocidadeCalculada < 0) {
            return velocidadeGps; // Movimento muito pequeno para calcular
        }

        // Se GPS e cálculo diferem muito, usar média
        double diferenca = Math.abs(velocidadeGps - velocidadeCalculada);
        if (diferenca > 10) { // Diferença maior que 10 km/h
            Log.d(TAG, String.format("Velocidades divergem - GPS: %.1f, Distância: %.1f, usando média: %.1f",
                    velocidadeGps, velocidadeCalculada, (velocidadeGps + velocidadeCalculada) / 2));
            return (velocidadeGps + velocidadeCalculada) / 2;
        }

        return velocidadeGps;
    }

    /**
     * Calcula velocidade baseado na distância percorrida
     */
    private double calcularVelocidadePorDistancia(Location location) {
        if (ultimaLocalizacao == null || ultimoTempo == null) {
            return -1;
        }

        // Calcular distância em metros
        float[] resultado = new float[1];
        Location.distanceBetween(
                ultimaLocalizacao.getLatitude(), ultimaLocalizacao.getLongitude(),
                location.getLatitude(), location.getLongitude(),
                resultado);
        double distanciaMetros = resultado[0];

        // Se movimento é muito pequeno (< 1 metro), ignorar
        if (distanciaMetros < 1.0) {
            return -1;
        }

        // Calcular tempo em segundos
        long tempoMs = System.currentTimeMillis() - ultimoTempo;
        double tempoSegundos = tempoMs / 1000.0;

        if (tempoSegundos < 0.1) { // Muito rápido
            return -1;
        }

        // Velocidade = distância / tempo
        // Converter m/s para km/h: * 3.6
        double velocidadeMs = distanciaMetros / tempoSegundos;
        return velocidadeMs * 3.6;
    }

    /**
     * Calcula média móvel das últimas leituras
     */
    private double calcularMediaMovel() {
        if (velocidadeBuffer.isEmpty()) {
            return 0;
        }

        // Dar mais peso às leituras recentes
        double soma = 0;
        double pesos = 0;

        for (int i = 0; i < velocidadeBuffer.size(); i++) {
            double peso = (i + 1) / (double) velocidadeBuffer.size();
            soma += velocidadeBuffer.get(i) * peso;
            pesos += peso;
        }

        return soma / pesos;
    }

    /**
     * Calcula média simples do buffer
     */
    private double calcularMedia() {
        if (velocidadeBuffer.isEmpty()) {
            return 0;
        }
        double soma = 0;
        for (double vel : velocidadeBuffer) {
            soma += vel;
        }
        return soma / velocidadeBuffer.size();
    }

    /**
     * Calcula desvio padrão do buffer
     */
    private double calcularDesviopadrao(double media) {
        if (velocidadeBuffer.isEmpty()) {
            return 0;
        }

        double somaDiferencas = 0;
        for (double vel : velocidadeBuffer) {
            somaDiferencas += Math.pow(vel - media, 2);
        }

        double variancia = somaDiferencas / velocidadeBuffer.size();
        return Math.sqrt(variancia);
    }

    /**
     * Retorna a velocidade máxima registrada
     */
    public double getVelocidadeMaxima() {
        return velocidadeMaxima;
    }

    /**
     * Retorna a velocidade média móvel atual
     */
    public double getVelocidadeAtual() {
        return velocidadeMediaMovel;
    }

    /**
     * Reseta o processador
     */
    public void reset() {
        velocidadeBuffer.clear();
        velocidadeMaxima = 0;
        velocidadeMediaMovel = 0;
        ultimoTempo = null;
        ultimaLocalizacao = null;
    }
}
