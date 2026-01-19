package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final UsuarioAdminRepository adminRepository;
    private final AtletaRepository atletaRepository;

    public UserDetailsServiceImpl(UsuarioAdminRepository adminRepository, AtletaRepository atletaRepository) {
        this.adminRepository = adminRepository;
        this.atletaRepository = atletaRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String identifier) {
        // 1. Tenta buscar como ADMIN
        return adminRepository.findByEmail(identifier)
                .map(admin -> User.builder()
                        .username(admin.getEmail())
                        .password(admin.getSenha())
                        .authorities("ROLE_ADMIN")
                        .build())
                .switchIfEmpty(Mono.defer(() -> buscarAtleta(identifier)));
    }

    private Mono<UserDetails> buscarAtleta(String identifier) {
        // Tenta buscar por e-mail ou por ID
        Mono<br.com.acervodaatletabrasileira.acervoapi.model.Atleta> atletaMono = identifier.contains("@")
                ? atletaRepository.findByEmail(identifier)
                : atletaRepository.findById(identifier);

        return atletaMono.map(this::buildAtletaUserDetails);
    }

    private UserDetails buildAtletaUserDetails(br.com.acervodaatletabrasileira.acervoapi.model.Atleta atleta) {
        // Recomendação: Use o e-mail como username para consistência nos Controllers
        return User.builder()
                .username(atleta.getEmail())
                .password(atleta.getSenha())
                .authorities("ROLE_ATLETA")
                .build();
    }
}