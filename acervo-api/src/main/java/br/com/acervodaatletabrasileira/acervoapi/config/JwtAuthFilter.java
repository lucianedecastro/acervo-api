// src/main/java/br/com/acervodaatletabrasileira/acervoapi/config/JwtAuthFilter.java
package br.com.acervodaatletabrasileira.acervoapi.config;

import br.com.acervodaatletabrasileira.acervoapi.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements WebFilter {

    @Autowired
    private JwtService jwtService;
    @Autowired
    private ReactiveUserDetailsService userDetailsService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange); // Se não tiver token, continua sem autenticar
        }

        String jwt = authHeader.substring(7);
        String username = jwtService.extractUsername(jwt);

        return userDetailsService.findByUsername(username)
                .flatMap(userDetails -> {
                    if (jwtService.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                        // Se o token for válido, autentica o usuário no contexto da requisição
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
                    }
                    return chain.filter(exchange); // Token inválido, continua sem autenticar
                })
                .switchIfEmpty(chain.filter(exchange)); // Usuário não encontrado, continua sem autenticar
    }
}
