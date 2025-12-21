package br.com.acervodaatletabrasileira.acervoapi.config;

import br.com.acervodaatletabrasileira.acervoapi.service.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements WebFilter {

    private final JwtService jwtService;
    private final ReactiveUserDetailsService userDetailsService;

    public JwtAuthFilter(
            JwtService jwtService,
            ReactiveUserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        // 🔓 Sem header ou sem Bearer → segue como request pública
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String jwt = authHeader.substring(7);

        return Mono.justOrEmpty(jwtService.extractUsername(jwt))
                .flatMap(username ->
                        userDetailsService.findByUsername(username)
                                .filter(userDetails ->
                                        jwtService.validateToken(jwt, userDetails)
                                )
                                .flatMap(userDetails -> {

                                    UsernamePasswordAuthenticationToken authentication =
                                            new UsernamePasswordAuthenticationToken(
                                                    userDetails,
                                                    null,
                                                    userDetails.getAuthorities()
                                            );

                                    // ✅ Injeta autenticação e deixa o fluxo seguir
                                    return chain.filter(exchange)
                                            .contextWrite(
                                                    ReactiveSecurityContextHolder
                                                            .withAuthentication(authentication)
                                            );
                                })
                )
                // ❗ Se qualquer etapa falhar, NÃO bloqueia aqui
                // Quem decide acesso é o SecurityConfig
                .switchIfEmpty(chain.filter(exchange));
    }
}
