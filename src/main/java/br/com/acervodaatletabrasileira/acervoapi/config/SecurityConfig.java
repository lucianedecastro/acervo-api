package br.com.acervodaatletabrasileira.acervoapi.config;

import br.com.acervodaatletabrasileira.acervoapi.service.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
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
@EnableReactiveMethodSecurity
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

    /* =====================================================
       AUTHENTICATION MANAGER
       ===================================================== */
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(
            UserDetailsServiceImpl userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        UserDetailsRepositoryReactiveAuthenticationManager authManager =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        authManager.setPasswordEncoder(passwordEncoder);
        return authManager;
    }

    /* =====================================================
       SECURITY FILTER CHAIN
       ===================================================== */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, e) ->
                                Mono.fromRunnable(() ->
                                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED)
                                )
                        )
                        .accessDeniedHandler((exchange, e) ->
                                Mono.fromRunnable(() ->
                                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN)
                                )
                        )
                )

                .authorizeExchange(exchanges -> {

                /* ==========================
                   1. PREFLIGHT (CORS)
                   ========================== */
                    exchanges.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                /* ==========================
                   2. AUTENTICAÇÃO
                   ========================== */
                    exchanges.pathMatchers(HttpMethod.POST, "/auth/login").permitAll();
                    exchanges.pathMatchers(HttpMethod.POST, "/admin/login").permitAll();

                    if (adminRegisterEnabled) {
                        exchanges.pathMatchers(HttpMethod.POST,
                                "/auth/register-admin",
                                "/auth/register-temp",
                                "/admin/register-temp"
                        ).permitAll();
                    }

                /* ==========================
                   3. SWAGGER / DOCUMENTAÇÃO
                   ========================== */
                    exchanges.pathMatchers(SWAGGER_WHITELIST).permitAll();

                /* ==========================
                   4. ROTAS PÚBLICAS (LEITURA)
                   ========================== */
                    exchanges.pathMatchers(HttpMethod.GET,
                            "/modalidades/**",
                            "/atletas/**",
                            "/acervo/**"
                    ).permitAll();

                /* ==========================
                   5. DASHBOARD ATLETA
                   ========================== */
                    exchanges.pathMatchers("/dashboard/atleta/**")
                            .hasRole("ATLETA");

                /* ==========================
                   6. EXTRATO E LICENCIAMENTO
                   ========================== */
                    exchanges.pathMatchers(
                            "/licenciamento/extrato/atleta/**",
                            "/licenciamento/simular",
                            "/licenciamento/efetivar"
                    ).hasAnyRole("ATLETA", "ADMIN");

                    exchanges.pathMatchers(
                            "/licenciamento/extrato/consolidado/**"
                    ).hasRole("ADMIN");

                /* ==========================
                   7. ADMINISTRATIVO
                   ========================== */
                    exchanges.pathMatchers(
                            "/admin/**",
                            "/dashboard/admin/**",
                            "/modalidades/admin/**",
                            "/atletas/admin/**",
                            "/configuracoes/**"
                    ).hasRole("ADMIN");

                /* ==========================
                   8. ESCRITA (PROTEGIDA)
                   ========================== */
                    exchanges.pathMatchers(
                            HttpMethod.POST,
                            "/acervo/**",
                            "/modalidades/**",
                            "/atletas/**"
                    ).hasRole("ADMIN");

                    exchanges.pathMatchers(
                            HttpMethod.PUT,
                            "/acervo/**",
                            "/modalidades/**",
                            "/atletas/**"
                    ).hasRole("ADMIN");

                    exchanges.pathMatchers(
                            HttpMethod.PATCH,
                            "/acervo/**",
                            "/modalidades/**",
                            "/atletas/**"
                    ).hasRole("ADMIN");

                    exchanges.pathMatchers(
                            HttpMethod.DELETE,
                            "/acervo/**",
                            "/modalidades/**",
                            "/atletas/**"
                    ).hasRole("ADMIN");

                /* ==========================
                   9. FALLBACK
                   ========================== */
                    exchanges.anyExchange().authenticated();
                })

                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

    /* =====================================================
       CORS
       ===================================================== */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://acervo-front-one.vercel.app",
                "https://www.acervodaatletabrasileira.com.br"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept"
        ));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /* =====================================================
       PASSWORD ENCODER
       ===================================================== */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
