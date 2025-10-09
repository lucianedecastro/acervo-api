package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;

@Service
public class UsuarioAdminService {

    @Autowired
    private UsuarioAdminRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Salva um novo admin no Firestore
    public Mono<UsuarioAdmin> save(UsuarioAdmin admin) {
        // Garantir que o ID (email) esteja preenchido
        if (admin.getEmail() == null || admin.getEmail().isEmpty()) {
            return Mono.error(new IllegalArgumentException("O email do admin é obrigatório"));
        }

        // CRIPTOGRAFAR a senha antes de salvar
        if (admin.getSenha() != null && !admin.getSenha().startsWith("$2a$")) {
            String senhaCriptografada = passwordEncoder.encode(admin.getSenha());
            admin.setSenha(senhaCriptografada);
        }

        // Preenche a data de criação se não estiver
        if (admin.getCriadoEm() == null) {
            admin.setCriadoEm(new Date());
        }

        return repository.save(admin);
    }

    // Busca admin por email
    public Mono<UsuarioAdmin> findByEmail(String email) {
        return repository.findById(email);
    }
}