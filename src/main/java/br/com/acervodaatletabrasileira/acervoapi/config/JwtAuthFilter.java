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

    public JwtAuthFilter(JwtService jwtService, ReactiveUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // 1. Verifica se o header está presente e no formato correto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String jwt = authHeader.substring(7);
        String userIdentifier; // Pode ser Email (Admin) ou ID (Atleta)

        try {
            userIdentifier = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Se o token estiver corrompido ou malformado, ignora
            return chain.filter(exchange);
        }

        if (userIdentifier != null) {
            // 2. Busca o usuário no banco (híbrido via UserDetailsServiceImpl)
            return userDetailsService.findByUsername(userIdentifier)
                    .flatMap(userDetails -> {
                        // 3. Valida se o token não expirou e se o identifier confere
                        if (jwtService.validateToken(jwt, userDetails)) {

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails,
                                            null,
                                            userDetails.getAuthorities()
                                    );

                            // 4. Injeta no Contexto Reativo e continua a filtragem
                            return chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                        }
                        return chain.filter(exchange);
                    })
                    // Se o usuário do Token não existir mais no banco
                    .switchIfEmpty(chain.filter(exchange));
        }

        return chain.filter(exchange);
    }
}