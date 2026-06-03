package br.jss.motoreviso.models;

import com.google.firebase.firestore.Exclude;

import java.util.List;

public class Manutencao {
    private String id;
    private String userId; // UID do proprietário da manutenção
    private String veiculoId;
    private Long dataRevisao;
    private Long kmRevisao;
    private Double custo;
    private String tipo;
    private String descricao;
    private String mecanico;
    private List<String> idsImagens;
    private List<String> pecasTrocadas;
    private Long proximaRevisaoKm;
    private Long proximaRevisaoData;
    private String notas;
    private Long dataCadastro;

    public Manutencao() {
    }

    public Manutencao(String veiculoId) {
        this.veiculoId = veiculoId;
        this.dataCadastro = System.currentTimeMillis();
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVeiculoId() {
        return veiculoId;
    }

    public void setVeiculoId(String veiculoId) {
        this.veiculoId = veiculoId;
    }

    public Long getDataRevisao() {
        return dataRevisao;
    }

    public void setDataRevisao(Long dataRevisao) {
        this.dataRevisao = dataRevisao;
    }

    public Long getKmRevisao() {
        return kmRevisao;
    }

    public void setKmRevisao(Long kmRevisao) {
        this.kmRevisao = kmRevisao;
    }

    public Double getCusto() {
        return custo;
    }

    public void setCusto(Double custo) {
        this.custo = custo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getMecanico() {
        return mecanico;
    }

    public void setMecanico(String mecanico) {
        this.mecanico = mecanico;
    }

    public List<String> getIdsImagens() {
        return idsImagens;
    }

    public void setIdsImagens(List<String> idsImagens) {
        this.idsImagens = idsImagens;
    }

    public List<String> getPecasTrocadas() {
        return pecasTrocadas;
    }

    public void setPecasTrocadas(List<String> pecasTrocadas) {
        this.pecasTrocadas = pecasTrocadas;
    }

    public Long getProximaRevisaoKm() {
        return proximaRevisaoKm;
    }

    public void setProximaRevisaoKm(Long proximaRevisaoKm) {
        this.proximaRevisaoKm = proximaRevisaoKm;
    }

    public Long getProximaRevisaoData() {
        return proximaRevisaoData;
    }

    public void setProximaRevisaoData(Long proximaRevisaoData) {
        this.proximaRevisaoData = proximaRevisaoData;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Long getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Long dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
}