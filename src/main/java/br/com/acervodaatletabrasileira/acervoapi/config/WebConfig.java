// src/main/java/.../config/WebConfig.java

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
        // Substitua pelo domínio REAL do seu Vercel (ex: acervo-front-one.vercel.app)
        final String VERCEL_DOMAIN = "https://acervo-front-one.vercel.app/";
        final String CUSTOM_DOMAIN = "https://acervodaatletabrasileira.com.br";

        // Adiciona a lista de domínios permitidos
        String[] allowedOrigins = {
                VERCEL_DOMAIN,
                CUSTOM_DOMAIN,
                "http://localhost:3000", // Para testes locais do React
                "http://127.0.0.1:3000"  // Outra porta comum de dev local
        };

        registry.addMapping("/**")
                .allowedOrigins(VERCEL_DOMAIN)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
