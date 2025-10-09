package br.com.acervodaatletabrasileira.acervoapi.dto;

public class AuthRequest {
    private String email;
    private String senha;

    // Construtor vazio obrigatório para desserialização
    public AuthRequest() {}

    public AuthRequest(String email, String senha) {
        this.email = email;
        this.senha = senha;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    @Override
    public String toString() {
        return "AuthRequest{" +
                "email='" + email + '\'' +
                ", senha='" + senha + '\'' +
                '}';
    }
}
