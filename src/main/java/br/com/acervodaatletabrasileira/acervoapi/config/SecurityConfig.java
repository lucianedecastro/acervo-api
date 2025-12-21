package br.com.acervodaatletabrasileira.acervoapi.config;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.admin.register-enabled:false}")
    private boolean adminRegisterEnabled;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    private static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/webjars/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeExchange(exchanges -> {

                    // ==========================
                    // Preflight (CORS)
                    // ==========================
                    exchanges.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // ==========================
                    // Login Admin
                    // ==========================
                    exchanges.pathMatchers(HttpMethod.POST, "/admin/login").permitAll();

                    // ==========================
                    // Seed admin (controlado por flag)
                    // ==========================
                    if (adminRegisterEnabled) {
                        exchanges.pathMatchers(
                                HttpMethod.POST,
                                "/admin/register-temp"
                        ).permitAll();
                    }

                    // ==========================
                    // Swagger / OpenAPI
                    // ==========================
                    exchanges.pathMatchers(SWAGGER_WHITELIST).permitAll();

                    // ==========================
                    // ROTAS PÚBLICAS (LEITURA)
                    // ==========================
                    exchanges.pathMatchers(HttpMethod.GET, "/modalidades/**").permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, "/atletas/**").permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, "/acervo/**").permitAll();

                    // ==========================
                    // ROTAS ADMIN / ESCRITA
                    // ==========================
                    exchanges.pathMatchers("/admin/**").authenticated();

                    exchanges.pathMatchers(HttpMethod.POST, "/modalidades/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.PUT, "/modalidades/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.DELETE, "/modalidades/**").authenticated();

                    exchanges.pathMatchers(HttpMethod.POST, "/atletas/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.PUT, "/atletas/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.DELETE, "/atletas/**").authenticated();

                    // ==========================
                    // Fallback
                    // ==========================
                    exchanges.anyExchange().authenticated();
                })

                // 🔑 JWT FILTER NO PONTO CORRETO
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHORIZATION);

        return http.build();
    }

    // ==========================
    // CORS
    // ==========================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://acervo-front-one.vercel.app",
                "https://www.acervodaatletabrasileira.com.br"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    // ==========================
    // Password Encoder
    // ==========================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
