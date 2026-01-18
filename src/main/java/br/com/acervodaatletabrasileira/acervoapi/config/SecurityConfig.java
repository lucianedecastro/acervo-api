package br.com.acervodaatletabrasileira.acervoapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

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

                // 🛠️ Ajuste de EntryPoint para evitar redirecionamentos que causam 401 no DELETE
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, e) ->
                                Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)))
                        .accessDeniedHandler((exchange, e) ->
                                Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN)))
                )

                .authorizeExchange(exchanges -> {

                    // ==========================
                    // Preflight (CORS)
                    // ==========================
                    exchanges.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // ==========================
                    // Login Admin / Seed
                    // ==========================
                    exchanges.pathMatchers(HttpMethod.POST, "/admin/login").permitAll();
                    if (adminRegisterEnabled) {
                        exchanges.pathMatchers(HttpMethod.POST, "/admin/register-temp").permitAll();
                    }

                    // ==========================
                    // Swagger / OpenAPI
                    // ==========================
                    exchanges.pathMatchers(SWAGGER_WHITELIST).permitAll();

                    // ==========================
                    // ROTAS PÚBLICAS
                    // ==========================
                    // Original (Apenas GET): exchanges.pathMatchers(HttpMethod.GET, "/licenciamento/**").permitAll();
                    exchanges.pathMatchers("/licenciamento/**").permitAll(); // Liberado para GET e POST (Teste de Proposta)

                    exchanges.pathMatchers(HttpMethod.GET, "/modalidades/**").permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, "/atletas/**").permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, "/acervo/**").permitAll();

                    // ==========================
                    // ROTAS ADMIN / ESCRITA (Agrupadas para maior clareza)
                    // ==========================
                    exchanges.pathMatchers("/admin/**").authenticated();

                    // Protege qualquer alteração (POST, PUT, DELETE) nestes caminhos
                    exchanges.pathMatchers("/modalidades/**").authenticated();
                    exchanges.pathMatchers("/atletas/**").authenticated();
                    exchanges.pathMatchers("/acervo/**").authenticated();

                    // ==========================
                    // Fallback
                    // ==========================
                    exchanges.anyExchange().authenticated();
                })

                // 🔑 JWT FILTER
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION);

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
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
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