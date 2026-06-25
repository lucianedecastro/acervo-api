package br.com.acervodaatletabrasileira.acervoapi.config;

import com.cloudinary.Cloudinary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class CloudinaryConfig {

    /**
     * Forma 1: string única (usada no application-local.yml).
     */
    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    /**
     * Forma 2: três chaves separadas (usada no application.yml de produção).
     */
    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String cloudinaryApiKey;

    @Value("${cloudinary.api-secret:}")
    private String cloudinaryApiSecret;

    @Bean
    public Cloudinary cloudinary() {

        if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
            return new Cloudinary(cloudinaryUrl);
        }

        if (!isBlank(cloudName) && !isBlank(cloudinaryApiKey) && !isBlank(cloudinaryApiSecret)) {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", cloudName);
            config.put("api_key", cloudinaryApiKey);
            config.put("api_secret", cloudinaryApiSecret);
            config.put("secure", true);
            return new Cloudinary(config);
        }

        // Isso vai impedir o erro de 'Placeholder' e deixar a aplicação subir
        // mas o upload vai falhar se você tentar usar. Ótimo para testar o boot!
        log.warn("CloudinaryConfig inicializado sem 'cloudinary.url' nem o trio cloud-name/api-key/api-secret. " +
                "Upload de imagens vai falhar até a configuração ser definida.");
        return new Cloudinary();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}