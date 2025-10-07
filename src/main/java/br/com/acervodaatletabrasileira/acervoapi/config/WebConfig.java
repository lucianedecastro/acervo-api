package br.com.acervodaatletabrasileira.acervoapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        final String VERCEL_DOMAIN = "https://acervo-front-one.vercel.app";
        final String CUSTOM_DOMAIN = "https://acervodaatletabrasileira.com.br";

        String[] allowedOrigins = {
                VERCEL_DOMAIN,
                CUSTOM_DOMAIN,
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        };

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
