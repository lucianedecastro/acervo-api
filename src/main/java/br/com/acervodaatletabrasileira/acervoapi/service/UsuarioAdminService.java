package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.UsuarioAdmin;
import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UsuarioAdminService {

    private final UsuarioAdminRepository repository;

    @Autowired
    public UsuarioAdminService(UsuarioAdminRepository repository) {
        this.repository = repository;
    }

    // Método que salva o novo administrador (chamado pelo AuthController)
    public Mono<UsuarioAdmin> save(UsuarioAdmin admin) {
        // O AuthController já criptografou a senha antes de chamar este método
        return repository.save(admin);
    }

    // Método usado pelo Spring Security para login
    public Mono<UsuarioAdmin> findByEmail(String email) {
        return repository.findByEmail(email);
    }
}