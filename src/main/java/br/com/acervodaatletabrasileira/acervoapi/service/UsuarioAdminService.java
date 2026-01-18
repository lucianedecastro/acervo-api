package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class UsuarioAdminService {

    private final UsuarioAdminRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioAdminService(
            UsuarioAdminRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==========================
    // CRIA / ATUALIZA ADMIN
    // ==========================
    public Mono<UsuarioAdmin> save(UsuarioAdmin admin) {

        if (admin.getEmail() == null || admin.getEmail().isBlank()) {
            return Mono.error(
                    new IllegalArgumentException("O email do admin é obrigatório")
            );
        }

        // Criptografa a senha somente se ainda não estiver criptografada
        if (admin.getSenha() != null && !admin.getSenha().startsWith("$2")) {
            admin.setSenha(passwordEncoder.encode(admin.getSenha()));
        }

        // ✅ CORREÇÃO: usar Instant (alinhado ao model)
        if (admin.getCriadoEm() == null) {
            admin.setCriadoEm(Instant.now());
        }

        return repository.save(admin);
    }

    // ==========================
    // BUSCA POR EMAIL
    // ==========================
    public Mono<UsuarioAdmin> findByEmail(String email) {
        return repository.findByEmail(email);
    }
}
