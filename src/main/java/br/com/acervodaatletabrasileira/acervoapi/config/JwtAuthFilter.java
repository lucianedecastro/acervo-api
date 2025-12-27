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
        String username = jwtService.extractUsername(jwt);

        if (username == null) {
            return chain.filter(exchange);
        }

        return userDetailsService.findByUsername(username)
                .flatMap(userDetails -> {
                    if (jwtService.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        // ✅ Alteração: Injetamos o contexto garantindo a persistência da auth
                        // mesmo em fluxos de retorno vazio (como o DELETE)
                        return chain.filter(exchange)
                                .contextWrite(context ->ReactiveSecurityContextHolder.withAuthentication(authentication));
                    }
                    return chain.filter(exchange);
                })
                // ❗ Se o usuário não for encontrado ou o fluxo esvaziar,
                // garantimos que a chain continue sem interromper a resposta.
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }
}