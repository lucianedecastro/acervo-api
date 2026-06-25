package br.com.acervodaatletabrasileira.acervoapi.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    @Bean
    public Cloudinary cloudinary() {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            // Isso vai impedir o erro de 'Placeholder' e deixar a aplicação subir
            // mas o upload vai falhar se você tentar usar. Ótimo para testar o boot!
            return new Cloudinary();
        }
        return new Cloudinary(cloudinaryUrl);
    }
}
