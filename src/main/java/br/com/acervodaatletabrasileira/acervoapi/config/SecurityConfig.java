package br.com.acervodaatletabrasileira.acervoapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    private static final String[] SWAGGER_WHITELIST = { "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**" };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // Rotas públicas (apenas leitura)
                        .pathMatchers(HttpMethod.GET, "/atletas/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/admin/login").permitAll()
                        .pathMatchers(SWAGGER_WHITELIST).permitAll()

                        // ✅ CORREÇÃO: Especifica que métodos de escrita e deleção em /atletas exigem autenticação.
                        .pathMatchers(HttpMethod.POST, "/atletas/**").authenticated()
                        .pathMatchers(HttpMethod.PUT, "/atletas/**").authenticated()
                        .pathMatchers(HttpMethod.DELETE, "/atletas/**").authenticated()

                        // Rotas admin genéricas
                        .pathMatchers("/admin/**").authenticated()

                        // Qualquer outra rota é negada por padrão (mais seguro)
                        .anyExchange().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

    // O restante do arquivo (corsConfigurationSource, passwordEncoder) permanece o mesmo...
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "https://acervo-front-one.vercel.app", "https://www.acervodaatletabrasileira.com.br"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}