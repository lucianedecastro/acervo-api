package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.Fotografa;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.FotografaRepository;
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
    private final FotografaRepository fotografaRepository;

    public UserDetailsServiceImpl(
            UsuarioAdminRepository adminRepository,
            AtletaRepository atletaRepository,
            FotografaRepository fotografaRepository
    ) {
        this.adminRepository = adminRepository;
        this.atletaRepository = atletaRepository;
        this.fotografaRepository = fotografaRepository;
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
                // 2. Tenta buscar como ATLETA
                .switchIfEmpty(Mono.defer(() -> buscarAtleta(identifier)))
                // 3. Tenta buscar como FOTOGRAFA
                .switchIfEmpty(Mono.defer(() -> buscarFotografa(identifier)));
    }

    private Mono<UserDetails> buscarAtleta(String identifier) {
        // Tenta buscar por e-mail ou por ID
        Mono<Atleta> atletaMono = identifier.contains("@")
                ? atletaRepository.findByEmail(identifier)
                : atletaRepository.findById(identifier);

        return atletaMono.map(this::buildAtletaUserDetails);
    }

    private UserDetails buildAtletaUserDetails(Atleta atleta) {
        // Recomendação: Use o e-mail como username para consistência nos Controllers
        return User.builder()
                .username(atleta.getEmail())
                .password(atleta.getSenha())
                .authorities("ROLE_ATLETA")
                .build();
    }

    private Mono<UserDetails> buscarFotografa(String identifier) {
        // Tenta buscar por e-mail ou por ID
        Mono<Fotografa> fotografaMono = identifier.contains("@")
                ? fotografaRepository.findByEmail(identifier)
                : fotografaRepository.findById(identifier);

        return fotografaMono.map(this::buildFotografaUserDetails);
    }

    private UserDetails buildFotografaUserDetails(Fotografa fotografa) {
        return User.builder()
                .username(fotografa.getEmail())
                .password(fotografa.getSenha())
                .authorities("ROLE_FOTOGRAFA")
                .build();
    }
}