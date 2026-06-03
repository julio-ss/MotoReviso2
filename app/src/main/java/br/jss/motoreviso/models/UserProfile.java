package br.jss.motoreviso.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class UserProfile {
    private String uid;
    private String nome;
    private String apelido;
    private String email;
    private String telefone;
    private String cidade;
    @ServerTimestamp
    private Date createdAt;

    public UserProfile() {
    }

    public UserProfile(String uid, String nome, String apelido, String email,
                      String telefone, String cidade) {
        this.uid = uid;
        this.nome = nome;
        this.apelido = apelido;
        this.email = email;
        this.telefone = telefone;
        this.cidade = cidade;
    }

    // Getters
    public String getUid() {
        return uid;
    }

    public String getNome() {
        return nome;
    }

    public String getApelido() {
        return apelido;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getCidade() {
        return cidade;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setApelido(String apelido) {
        this.apelido = apelido;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
