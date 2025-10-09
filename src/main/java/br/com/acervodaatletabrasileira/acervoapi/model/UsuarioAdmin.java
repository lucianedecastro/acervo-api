package br.com.acervodaatletabrasileira.acervoapi.model;

import com.google.cloud.spring.data.firestore.Document;
import com.google.cloud.firestore.annotation.DocumentId;
import java.util.Date;

@Document(collectionName = "admins")
public class UsuarioAdmin {

    @DocumentId
    private String email; // agora o Firestore reconhece este campo como ID

    private String senha;
    private String role;
    private Date criadoEm;

    public UsuarioAdmin() {}

    public UsuarioAdmin(String email, String senha, String role) {
        this.email = email;
        this.senha = senha;
        this.role = role;
        this.criadoEm = new Date();
    }

    // Getters e Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Date getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Date criadoEm) { this.criadoEm = criadoEm; }

    @Override
    public String toString() {
        return "UsuarioAdmin{" +
                "email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", criadoEm=" + criadoEm +
                '}';
    }
}
