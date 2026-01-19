package br.com.acervodaatletabrasileira.acervoapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admins")
public class UsuarioAdmin {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String senha;

    /**
     * Exemplo: ROLE_ADMIN
     */
    private String role;

    private Instant criadoEm;

    // ==========================
    // Construtor de conveniência
    // ==========================
    public UsuarioAdmin(String email, String senha, String role) {
        this.email = email;
        this.senha = senha;
        // Garante o prefixo ROLE_ caso venha apenas "ADMIN"
        this.role = (role != null && !role.startsWith("ROLE_")) ? "ROLE_" + role : role;
        this.criadoEm = Instant.now();
    }
}