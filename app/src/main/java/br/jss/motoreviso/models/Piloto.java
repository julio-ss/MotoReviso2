package br.jss.motoreviso.models;

import com.google.firebase.firestore.Exclude;

public class Piloto {

    @Exclude
    private String id;
    private String userId;

    // Dados pessoais
    private String nomeCompleto;
    private String apelido;
    private String numeroPiloto;
    private Long dataNascimento;
    private Double peso;
    private Double altura;
    private String tipoSanguineo;
    private String nacionalidade;
    private String cidade;
    private String telefone;
    private String email;
    private String contatoEmergencia;

    // Pilotagem
    private String categoria;
    private Long anosExperiencia;
    private String veiculoId;
    private String equipe;
    private Long numCorridas;
    private Long numVitorias;
    private Long numQuedas;
    private String melhorTempo;
    private String observacoes;

    // Avatar
    private String avatarCor1;
    private String avatarCor2;
    private String capacete;  // "integral", "modular", "offroad", "agv_real", "custom"
    private String jaqueta;   // "racing", "street", "touring", "dainese_real", "custom"
    private String calca;     // "racing", "street", "touring", "dainese_real", "custom"
    private String botas;     // "racing", "street", "touring", "dainese_real", "custom"
    private String capaceteImageUrl;
    private String jacuetaImageUrl;
    private String calcaImageUrl;
    private String botasImageUrl;
    private String avatarPreviewPath;   // Caminho local do bitmap gerado do avatar

    // Foto
    private String fotoUrl;

    private Long dataCadastro;

    // Construtor vazio obrigatório para Firestore
    public Piloto() {}

    public Piloto(String nomeCompleto, String numeroPiloto, String categoria) {
        this.nomeCompleto = nomeCompleto;
        this.numeroPiloto = numeroPiloto;
        this.categoria = categoria;
        this.dataCadastro = System.currentTimeMillis();
    }

    // ─── Getters e Setters ───────────────────────────────────────

    @Exclude
    public String getId() { return id; }
    @Exclude
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }

    public String getApelido() { return apelido; }
    public void setApelido(String apelido) { this.apelido = apelido; }

    public String getNumeroPiloto() { return numeroPiloto; }
    public void setNumeroPiloto(String numeroPiloto) { this.numeroPiloto = numeroPiloto; }

    public Long getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(Long dataNascimento) { this.dataNascimento = dataNascimento; }

    public Double getPeso() { return peso; }
    public void setPeso(Double peso) { this.peso = peso; }

    public Double getAltura() { return altura; }
    public void setAltura(Double altura) { this.altura = altura; }

    public String getTipoSanguineo() { return tipoSanguineo; }
    public void setTipoSanguineo(String tipoSanguineo) { this.tipoSanguineo = tipoSanguineo; }

    public String getNacionalidade() { return nacionalidade; }
    public void setNacionalidade(String nacionalidade) { this.nacionalidade = nacionalidade; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContatoEmergencia() { return contatoEmergencia; }
    public void setContatoEmergencia(String contatoEmergencia) { this.contatoEmergencia = contatoEmergencia; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public Long getAnosExperiencia() { return anosExperiencia; }
    public void setAnosExperiencia(Long anosExperiencia) { this.anosExperiencia = anosExperiencia; }

    public String getVeiculoId() { return veiculoId; }
    public void setVeiculoId(String veiculoId) { this.veiculoId = veiculoId; }

    public String getEquipe() { return equipe; }
    public void setEquipe(String equipe) { this.equipe = equipe; }

    public Long getNumCorridas() { return numCorridas; }
    public void setNumCorridas(Long numCorridas) { this.numCorridas = numCorridas; }

    public Long getNumVitorias() { return numVitorias; }
    public void setNumVitorias(Long numVitorias) { this.numVitorias = numVitorias; }

    public Long getNumQuedas() { return numQuedas; }
    public void setNumQuedas(Long numQuedas) { this.numQuedas = numQuedas; }

    public String getMelhorTempo() { return melhorTempo; }
    public void setMelhorTempo(String melhorTempo) { this.melhorTempo = melhorTempo; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public String getAvatarCor1() { return avatarCor1; }
    public void setAvatarCor1(String avatarCor1) { this.avatarCor1 = avatarCor1; }

    public String getAvatarCor2() { return avatarCor2; }
    public void setAvatarCor2(String avatarCor2) { this.avatarCor2 = avatarCor2; }

    public String getCapacete() { return capacete; }
    public void setCapacete(String capacete) { this.capacete = capacete; }

    public String getJaqueta() { return jaqueta; }
    public void setJaqueta(String jaqueta) { this.jaqueta = jaqueta; }

    public String getCalca() { return calca; }
    public void setCalca(String calca) { this.calca = calca; }

    public String getBotas() { return botas; }
    public void setBotas(String botas) { this.botas = botas; }

    public String getCapaceteImageUrl() { return capaceteImageUrl; }
    public void setCapaceteImageUrl(String capaceteImageUrl) { this.capaceteImageUrl = capaceteImageUrl; }

    public String getJacuetaImageUrl() { return jacuetaImageUrl; }
    public void setJacuetaImageUrl(String jacuetaImageUrl) { this.jacuetaImageUrl = jacuetaImageUrl; }

    public String getCalcaImageUrl() { return calcaImageUrl; }
    public void setCalcaImageUrl(String calcaImageUrl) { this.calcaImageUrl = calcaImageUrl; }

    public String getBotasImageUrl() { return botasImageUrl; }
    public void setBotasImageUrl(String botasImageUrl) { this.botasImageUrl = botasImageUrl; }

    public String getAvatarPreviewPath() { return avatarPreviewPath; }
    public void setAvatarPreviewPath(String avatarPreviewPath) { this.avatarPreviewPath = avatarPreviewPath; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public Long getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(Long dataCadastro) { this.dataCadastro = dataCadastro; }

    // ─── Métodos utilitários ─────────────────────────────────────

    /** Retorna a idade calculada a partir de dataNascimento, ou -1 se não definida */
    @Exclude
    public int getIdade() {
        if (dataNascimento == null) return -1;
        java.util.Calendar nascimento = java.util.Calendar.getInstance();
        nascimento.setTimeInMillis(dataNascimento);
        java.util.Calendar hoje = java.util.Calendar.getInstance();
        int idade = hoje.get(java.util.Calendar.YEAR) - nascimento.get(java.util.Calendar.YEAR);
        if (hoje.get(java.util.Calendar.DAY_OF_YEAR) < nascimento.get(java.util.Calendar.DAY_OF_YEAR)) {
            idade--;
        }
        return idade;
    }

    /** Retorna o nome de exibição preferido (apelido se disponível, senão nome completo) */
    @Exclude
    public String getNomeExibicao() {
        if (apelido != null && !apelido.trim().isEmpty()) return apelido;
        return nomeCompleto != null ? nomeCompleto : "";
    }
}
