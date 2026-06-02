package br.jss.motoreviso.models;

import com.google.firebase.firestore.Exclude;

public class Veiculo {
    private String id;
    private String tipo; // moto ou carro
    private String marca;
    private String modelo;
    private Long ano;
    private String placa;
    private String chassis;
    private String motor;
    private String combustivel;
    private String cambio;
    private Long potencia;
    private String torque;
    private Long cilindrada;
    private Long peso;
    private String dimensoes;
    private Long tanque;
    private Long kmAtual;
    private Long kmTroca;
    private Long dataCadastro;
    private Long dataUltimaRevisao;
    private Long dataProximaRevisao;  // Data agendada para próxima revisão
    private String urlImagemPrincipal;
    private Long intervaloRevisao; // padrão 5000 km
    private String descricao;
    private Boolean ativo;

    // Health indicators (0-100%)
    private Long healthOleo;      // Oil health
    private Long healthPneus;     // Tire health
    private Long healthFreios;    // Brake health
    private Long healthCorrente;  // Chain health (motorcycle specific)

    public Veiculo() {
    }

    public Veiculo(String tipo, String marca, String modelo, String placa) {
        this.tipo = tipo;
        this.marca = marca;
        this.modelo = modelo;
        this.placa = placa;
        this.ativo = true;
        this.intervaloRevisao = 5000L;
        this.dataCadastro = System.currentTimeMillis();
    }

    @Exclude
    public Long getKmParaProximaRevisao() {
        if (kmAtual == null || kmTroca == null || intervaloRevisao == null) {
            return intervaloRevisao != null ? intervaloRevisao : 5000L;
        }
        Long proxima = kmTroca + intervaloRevisao;
        return Math.max(0, proxima - kmAtual);
    }

    @Exclude
    public Boolean precisaRevisao() {
        if (kmAtual == null || kmTroca == null) {
            return false;
        }
        Long proximaRevisao = kmTroca + (intervaloRevisao != null ? intervaloRevisao : 5000L);
        return kmAtual >= (proximaRevisao - 500); // Alerta com 500 km de antecedência
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Long getAno() {
        return ano;
    }

    public void setAno(Long ano) {
        this.ano = ano;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getChassis() {
        return chassis;
    }

    public void setChassis(String chassis) {
        this.chassis = chassis;
    }

    public String getMotor() {
        return motor;
    }

    public void setMotor(String motor) {
        this.motor = motor;
    }

    public String getCombustivel() {
        return combustivel;
    }

    public void setCombustivel(String combustivel) {
        this.combustivel = combustivel;
    }

    public String getCambio() {
        return cambio;
    }

    public void setCambio(String cambio) {
        this.cambio = cambio;
    }

    public Long getPotencia() {
        return potencia;
    }

    public void setPotencia(Long potencia) {
        this.potencia = potencia;
    }

    public String getTorque() {
        return torque;
    }

    public void setTorque(String torque) {
        this.torque = torque;
    }

    public Long getCilindrada() {
        return cilindrada;
    }

    public void setCilindrada(Long cilindrada) {
        this.cilindrada = cilindrada;
    }

    public Long getPeso() {
        return peso;
    }

    public void setPeso(Long peso) {
        this.peso = peso;
    }

    public String getDimensoes() {
        return dimensoes;
    }

    public void setDimensoes(String dimensoes) {
        this.dimensoes = dimensoes;
    }

    public Long getTanque() {
        return tanque;
    }

    public void setTanque(Long tanque) {
        this.tanque = tanque;
    }

    public Long getKmAtual() {
        return kmAtual;
    }

    public void setKmAtual(Long kmAtual) {
        this.kmAtual = kmAtual;
    }

    public Long getKmTroca() {
        return kmTroca;
    }

    public void setKmTroca(Long kmTroca) {
        this.kmTroca = kmTroca;
    }

    public Long getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Long dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public Long getDataUltimaRevisao() {
        return dataUltimaRevisao;
    }

    public void setDataUltimaRevisao(Long dataUltimaRevisao) {
        this.dataUltimaRevisao = dataUltimaRevisao;
    }

    public Long getDataProximaRevisao() {
        return dataProximaRevisao;
    }

    public void setDataProximaRevisao(Long dataProximaRevisao) {
        this.dataProximaRevisao = dataProximaRevisao;
    }

    public String getUrlImagemPrincipal() {
        return urlImagemPrincipal;
    }

    public void setUrlImagemPrincipal(String urlImagemPrincipal) {
        this.urlImagemPrincipal = urlImagemPrincipal;
    }

    public Long getIntervaloRevisao() {
        return intervaloRevisao;
    }

    public void setIntervaloRevisao(Long intervaloRevisao) {
        this.intervaloRevisao = intervaloRevisao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    // Health Indicators
    public Long getHealthOleo() {
        return healthOleo != null ? healthOleo : 0L;
    }

    public void setHealthOleo(Long healthOleo) {
        this.healthOleo = healthOleo;
    }

    public Long getHealthPneus() {
        return healthPneus != null ? healthPneus : 0L;
    }

    public void setHealthPneus(Long healthPneus) {
        this.healthPneus = healthPneus;
    }

    public Long getHealthFreios() {
        return healthFreios != null ? healthFreios : 0L;
    }

    public void setHealthFreios(Long healthFreios) {
        this.healthFreios = healthFreios;
    }

    public Long getHealthCorrente() {
        return healthCorrente != null ? healthCorrente : 0L;
    }

    public void setHealthCorrente(Long healthCorrente) {
        this.healthCorrente = healthCorrente;
    }
}