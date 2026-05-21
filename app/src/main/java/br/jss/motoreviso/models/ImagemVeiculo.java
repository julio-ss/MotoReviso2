package br.jss.motoreviso.models;

public class ImagemVeiculo {
    private String id;
    private String veiculoId;
    private String urlImagem;
    private String descricao;
    private Long dataCadastro;
    private Boolean capa;
    private String categoria;

    public ImagemVeiculo() {
    }

    public ImagemVeiculo(String veiculoId, String urlImagem) {
        this.veiculoId = veiculoId;
        this.urlImagem = urlImagem;
        this.dataCadastro = System.currentTimeMillis();
        this.capa = false;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(String veiculoId) {
        this.veiculoId = veiculoId;
    }

    public String getUrlImagem() {
        return urlImagem;
    }

    public void setUrlImagem(String urlImagem) {
        this.urlImagem = urlImagem;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Long getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Long dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Boolean getCapa() {
        return capa;
    }

    public void setCapa(Boolean capa) {
        this.capa = capa;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}