package br.jss.motoreviso.models;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.List;

public class Trajeto {
    private String id;
    private String userId;
    private String veiculoId;
    private Long dataInicio;
    private Long dataFim;
    private Long kmInicial;
    private Long kmFinal;
    private Double kmRodados;
    private Double velocidadeMaxima;
    private Double velocidadeMedia;
    private List<Ponto> pontos;
    private String origem;
    private String destino;
    private Long duracao; // em minutos
    private Long dataCadastro;

    public Trajeto() {
        this.pontos = new ArrayList<>();
        this.dataCadastro = System.currentTimeMillis();
    }

    public Trajeto(String veiculoId) {
        this.veiculoId = veiculoId;
        this.pontos = new ArrayList<>();
        this.dataCadastro = System.currentTimeMillis();
    }

    @Exclude
    public void finalizarTrajeto(java.util.Date dataFim, Long kmFinal, Double velMax) {
        this.dataFim = dataFim.getTime();
        this.kmFinal = kmFinal;
        this.velocidadeMaxima = velMax;
        if (kmInicial != null && kmFinal != null) {
            this.kmRodados = (double) (kmFinal - kmInicial);
        }
        if (dataInicio != null && this.dataFim != null) {
            this.duracao = (this.dataFim - dataInicio) / 60000;
        }
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getVeiculoId() { return veiculoId; }
    public void setVeiculoId(String veiculoId) { this.veiculoId = veiculoId; }

    public Long getDataInicio() { return dataInicio; }
    public void setDataInicio(Long dataInicio) { this.dataInicio = dataInicio; }

    public Long getDataFim() { return dataFim; }
    public void setDataFim(Long dataFim) { this.dataFim = dataFim; }

    public Long getKmInicial() { return kmInicial; }
    public void setKmInicial(Long kmInicial) { this.kmInicial = kmInicial; }

    public Long getKmFinal() { return kmFinal; }
    public void setKmFinal(Long kmFinal) { this.kmFinal = kmFinal; }

    public Double getKmRodados() { return kmRodados; }
    public void setKmRodados(Double kmRodados) { this.kmRodados = kmRodados; }

    public Double getVelocidadeMaxima() { return velocidadeMaxima; }
    public void setVelocidadeMaxima(Double velocidadeMaxima) { this.velocidadeMaxima = velocidadeMaxima; }

    public Double getVelocidadeMedia() { return velocidadeMedia; }
    public void setVelocidadeMedia(Double velocidadeMedia) { this.velocidadeMedia = velocidadeMedia; }

    public List<Ponto> getPontos() { return pontos; }
    public void setPontos(List<Ponto> pontos) { this.pontos = pontos; }

    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public Long getDuracao() { return duracao; }
    public void setDuracao(Long duracao) { this.duracao = duracao; }

    public Long getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(Long dataCadastro) { this.dataCadastro = dataCadastro; }

    @Exclude
    public void setDistanciaKm(double km) { this.kmRodados = km; }

    @Exclude
    public void setDuracaoMs(long ms) { this.duracao = ms / 60000; }

    public static class Ponto {
        private Double latitude;
        private Double longitude;
        private Double velocidade;
        private Long timestamp;

        public Ponto() {}

        public Ponto(Double latitude, Double longitude, Double velocidade) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.velocidade = velocidade;
            this.timestamp = System.currentTimeMillis();
        }

        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }

        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }

        public Double getVelocidade() { return velocidade; }
        public void setVelocidade(Double velocidade) { this.velocidade = velocidade; }

        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}
