package br.jss.motoreviso.models;

import com.google.firebase.firestore.Exclude;

public class Equipamento {

    @Exclude
    private String id;
    private String userId;
    private String pilotoId;

    private String tipo;
    private String marca;
    private String modelo;
    private String tamanho;
    private String cor;
    private String notas;
    private String fotoUrl;

    private Long dataCompra;
    private Long dataValidade;
    private Long dataCadastro;

    // Construtor vazio obrigatório para Firestore
    public Equipamento() {}

    public Equipamento(String pilotoId, String tipo, String marca, String modelo) {
        this.pilotoId = pilotoId;
        this.tipo = tipo;
        this.marca = marca;
        this.modelo = modelo;
        this.dataCadastro = System.currentTimeMillis();
    }

    // ─── Getters e Setters ───────────────────────────────────────

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPilotoId() { return pilotoId; }
    public void setPilotoId(String pilotoId) { this.pilotoId = pilotoId; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getTamanho() { return tamanho; }
    public void setTamanho(String tamanho) { this.tamanho = tamanho; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public Long getDataCompra() { return dataCompra; }
    public void setDataCompra(Long dataCompra) { this.dataCompra = dataCompra; }

    public Long getDataValidade() { return dataValidade; }
    public void setDataValidade(Long dataValidade) { this.dataValidade = dataValidade; }

    public Long getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(Long dataCadastro) { this.dataCadastro = dataCadastro; }

    // ─── Métodos utilitários ─────────────────────────────────────

    /**
     * Retorna os dias restantes até a validade.
     * Negativo = vencido. null = sem data de validade definida.
     */
    @Exclude
    public Long getDiasParaVencer() {
        if (dataValidade == null) return null;
        long diff = dataValidade - System.currentTimeMillis();
        return diff / (1000L * 60 * 60 * 24);
    }

    /**
     * 0 = válido (>30 dias), 1 = atenção (≤30 dias), 2 = vencido (≤0 ou sem data)
     */
    @Exclude
    public int getStatusValidade() {
        Long dias = getDiasParaVencer();
        if (dias == null || dias <= 0) return 2;
        if (dias <= 30) return 1;
        return 0;
    }
}
