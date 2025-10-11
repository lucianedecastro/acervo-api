package br.com.acervodaatletabrasileira.acervoapi.config;

import br.com.acervodaatletabrasileira.acervoapi.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    @Autowired
    public JwtAuthFilter(JwtService jwtService, ReactiveUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Se não há token, deixa passar para as rotas públicas ou para o Spring Security barrar depois.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String jwt = authHeader.substring(7);
        String username;
        try {
            // Tenta extrair o nome de usuário. Se o token for malformado, vai falhar.
            username = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // ✅ Se o token é ilegível, barra a requisição imediatamente.
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // Se o nome de usuário foi extraído, busca no banco.
        return userDetailsService.findByUsername(username)
                .flatMap(userDetails -> {
                    // Se o usuário foi encontrado, valida o token (assinatura e expiração).
                    if (jwtService.validateToken(jwt, userDetails)) {
                        // ✅ Se o token é válido, adiciona a autenticação e continua.
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authToken));
                    } else {
                        // ✅ Se o token é inválido (ex: expirado), barra a requisição.
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // ✅ Se o usuário do token não existe mais no banco, barra a requisição.
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }));
    }
}