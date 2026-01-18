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
        // 1. Tenta buscar como ADMIN (sempre pelo e-mail)
        return adminRepository.findByEmail(identifier)
                .map(admin -> User.builder()
                        .username(admin.getEmail())
                        .password(admin.getSenha())
                        .authorities("ROLE_ADMIN")
                        .build())
                .switchIfEmpty(Mono.defer(() -> {
                    // 2. Se não for admin, tenta buscar como ATLETA
                    // O identificador pode ser o E-mail (no login) ou o ID (no JWT)

                    if (identifier.contains("@")) {
                        return atletaRepository.findByEmail(identifier)
                                .map(atleta -> buildAtletaUserDetails(atleta));
                    } else {
                        return atletaRepository.findById(identifier)
                                .map(atleta -> buildAtletaUserDetails(atleta));
                    }
                }));
    }

    /**
     * Centraliza a construção do UserDetails da Atleta para evitar repetição.
     * Usamos o ID como username no UserDetails para que o JwtService o coloque no subject do token.
     */
    private UserDetails buildAtletaUserDetails(br.com.acervodaatletabrasileira.acervoapi.model.Atleta atleta) {
        return User.builder()
                .username(atleta.getId())
                .password(atleta.getSenha())
                .authorities("ROLE_ATLETA")
                .build();
    }
}