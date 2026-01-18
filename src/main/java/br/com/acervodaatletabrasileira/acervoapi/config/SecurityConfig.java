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
@EnableReactiveMethodSecurity // Ativa a proteção por anotações como @PreAuthorize
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

    /**
     * Define o Gerenciador de Autenticação Reativo.
     * Ele usa o seu UserDetailsServiceImpl (híbrido) e o BCrypt para validar as senhas.
     */
    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(
            UserDetailsServiceImpl userDetailsService,
            PasswordEncoder passwordEncoder) {
        UserDetailsRepositoryReactiveAuthenticationManager authManager =
                new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        authManager.setPasswordEncoder(passwordEncoder);
        return authManager;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

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
                    // Login / Registro Livre (PORTA DE ENTRADA)
                    // ==========================
                    exchanges.pathMatchers(HttpMethod.POST, "/admin/login").permitAll();
                    exchanges.pathMatchers(HttpMethod.POST, "/atletas").permitAll(); // Cadastro de Atletas Livre

                    if (adminRegisterEnabled) {
                        exchanges.pathMatchers(HttpMethod.POST, "/admin/register-temp").permitAll();
                    }

                    // ==========================
                    // Swagger / OpenAPI
                    // ==========================
                    exchanges.pathMatchers(SWAGGER_WHITELIST).permitAll();

                    // ==========================
                    // ROTAS PÚBLICAS (Frente de Pesquisa / GETs)
                    // ==========================
                    exchanges.pathMatchers("/licenciamento/**").permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, "/modalidades/**").permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, "/atletas/**").permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, "/acervo/**").permitAll();

                    // ==========================
                    // DASHBOARDS (Protegidos por Roles no Controller)
                    // ==========================
                    exchanges.pathMatchers("/api/dashboard/**").authenticated();

                    // ==========================
                    // ROTAS DE GESTÃO (ATLETAS)
                    // ==========================
                    exchanges.pathMatchers(HttpMethod.PUT, "/atletas/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.PATCH, "/atletas/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.DELETE, "/atletas/**").authenticated();

                    // ==========================
                    // ROTAS DE GESTÃO (ACERVO)
                    // ==========================
                    exchanges.pathMatchers(HttpMethod.POST, "/acervo/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.PUT, "/acervo/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.DELETE, "/acervo/**").authenticated();

                    exchanges.pathMatchers("/admin/**").authenticated();
                    exchanges.pathMatchers("/modalidades/**").authenticated();

                    // ==========================
                    // Fallback
                    // ==========================
                    exchanges.anyExchange().authenticated();
                })

                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://acervo-front-one.vercel.app",
                "https://www.acervodaatletabrasileira.com.br"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
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