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

                    // 1. Preflight (CORS)
                    exchanges.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // 2. LOGIN (Sempre Livre)
                    exchanges.pathMatchers(HttpMethod.POST, "/auth/login").permitAll();
                    exchanges.pathMatchers(HttpMethod.POST, "/admin/login").permitAll();

                    // 3. CADASTRO DE ATLETAS (Sempre Livre)
                    exchanges.pathMatchers(HttpMethod.POST, "/atletas").permitAll();

                    // 4. REGISTROS ADMINISTRATIVOS (Protegidos por Flag de Ambiente)
                    if (adminRegisterEnabled) {
                        exchanges.pathMatchers(HttpMethod.POST, "/auth/register-admin").permitAll();
                        exchanges.pathMatchers(HttpMethod.POST, "/auth/register-temp").permitAll();
                        exchanges.pathMatchers(HttpMethod.POST, "/admin/register-temp").permitAll();
                    }

                    // 5. DOCUMENTAÇÃO E CONSULTA PÚBLICA
                    exchanges.pathMatchers(SWAGGER_WHITELIST).permitAll();
                    exchanges.pathMatchers("/licenciamento/**").permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, "/modalidades/**").permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, "/atletas/**").permitAll();
                    exchanges.pathMatchers(HttpMethod.GET, "/acervo/**").permitAll();

                    // 6. DASHBOARDS (Protegidos - Exige Token)
                    exchanges.pathMatchers("/dashboard/**").authenticated();
                    exchanges.pathMatchers("/api/dashboard/**").authenticated();

                    // 7. GESTÃO DE ATLETAS E ACERVO (Escrita)
                    exchanges.pathMatchers(HttpMethod.PUT, "/atletas/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.PATCH, "/atletas/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.DELETE, "/atletas/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.POST, "/acervo/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.PUT, "/acervo/**").authenticated();
                    exchanges.pathMatchers(HttpMethod.DELETE, "/acervo/**").authenticated();

                    // 8. ÁREA ADMINISTRATIVA
                    exchanges.pathMatchers("/admin/**").authenticated();
                    exchanges.pathMatchers("/modalidades/**").authenticated();

                    // 9. FALLBACK
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