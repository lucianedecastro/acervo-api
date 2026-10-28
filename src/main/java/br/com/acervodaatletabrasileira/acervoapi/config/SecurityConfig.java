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

    @Value("${cors.allowed-origins}")
    private List<String> corsAllowedOrigins;

    @Value("${cors.allow-credentials:true}")
    private boolean corsAllowCredentials;

    @Value("${app.swagger.enabled:false}")
    private boolean swaggerEnabled;

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
                    exchanges.pathMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll();

                    exchanges.pathMatchers(HttpMethod.POST, "/auth/login").permitAll();
                    exchanges.pathMatchers(HttpMethod.POST, "/admin/login").permitAll();

                /* ==========================
                   2b. AUTOCADASTRO FOTÓGRAFA
                   ========================== */
                    exchanges.pathMatchers(HttpMethod.POST, "/fotografas/cadastro").permitAll();

                /* ==========================
                   2b2. AUTOCADASTRO ATLETA
                   ========================== */
                    exchanges.pathMatchers(HttpMethod.POST, "/atletas/registro").permitAll();

                /* ==========================
                   2c. WEBHOOK ASAAS
                   ========================== */
                    // Rota pública por natureza (o gateway não envia JWT).
                    // Autenticidade é validada no controller via header próprio.
                    exchanges.pathMatchers(HttpMethod.POST, "/webhooks/asaas").permitAll();

                    if (adminRegisterEnabled) {
                        // Bootstrap do primeiro admin: só deve ficar ligado temporariamente.
                        exchanges.pathMatchers(HttpMethod.POST,
                                "/auth/register-admin",
                                "/auth/register-temp",
                                "/admin/register-temp"
                        ).permitAll();
                    } else {
                        // Com a flag desligada, apenas um ADMIN já autenticado pode criar outro.
                        // Sem esta regra a rota cairia em authenticated() e qualquer
                        // ATLETA/FOTOGRAFA logada conseguiria criar um ADMIN.
                        exchanges.pathMatchers(HttpMethod.POST,
                                "/auth/register-admin",
                                "/auth/register-temp"
                        ).hasRole("ADMIN");
                    }

                /* ==========================
                   3. SWAGGER / DOCUMENTAÇÃO
                   ========================== */
                    // Documentação só é pública quando habilitada explicitamente (SWAGGER_ENABLED=true).
                    // Desligada, o springdoc nem registra as rotas e a whitelist não é aberta.
                    if (swaggerEnabled) {
                        exchanges.pathMatchers(SWAGGER_WHITELIST).permitAll();
                    }

                /* ==========================
                   3b. ADMINISTRATIVO
                   ========================== */
                    // Deve ser avaliado ANTES das leituras públicas (GET /modalidades/**,
                    // /atletas/**, /acervo/**), senão as rotas /admin/** ficam públicas.
                    exchanges.pathMatchers(
                            "/admin/**",
                            "/dashboard/admin/**",
                            "/modalidades/admin/**",
                            "/atletas/admin/**",
                            "/acervo/admin/**",
                            "/licenciamento/admin/**",
                            "/juridico/**",
                            "/configuracoes/**"
                    ).hasRole("ADMIN");

                /* ==========================
                   4. ROTAS PÚBLICAS (LEITURA)
                   ========================== */
                    // Avaliada ANTES do permitAll genérico de /fotografas/**,
                    // senão a rota privada da fotógrafa logada ficaria pública.
                    exchanges.pathMatchers(HttpMethod.GET, "/fotografas/me").hasRole("FOTOGRAFA");

                    exchanges.pathMatchers(HttpMethod.GET,
                            "/modalidades/**",
                            "/atletas/**",
                            "/acervo/**",
                            "/fotografas/**"
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
                            "/atletas/**",
                            "/fotografas/**"
                    ).hasRole("ADMIN");

                    exchanges.pathMatchers(
                            HttpMethod.PATCH,
                            "/acervo/**",
                            "/modalidades/**",
                            "/atletas/**",
                            "/fotografas/**"
                    ).hasRole("ADMIN");

                    exchanges.pathMatchers(
                            HttpMethod.DELETE,
                            "/acervo/**",
                            "/modalidades/**",
                            "/atletas/**",
                            "/fotografas/**"
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

        // Origens vêm de cors.allowed-origins (CORS_ALLOWED_ORIGINS), separadas por vírgula.
        // "*" é descartado: não pode ser combinado com credenciais.
        config.setAllowedOrigins(corsAllowedOrigins.stream()
                .map(String::trim)
                .filter(o -> !o.isEmpty() && !"*".equals(o))
                .toList());

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept"
        ));

        config.setAllowCredentials(corsAllowCredentials);

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