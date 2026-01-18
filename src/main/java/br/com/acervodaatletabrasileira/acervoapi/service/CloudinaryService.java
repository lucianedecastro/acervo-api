package br.com.acervodaatletabrasileira.acervoapi.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Serviço responsável pelo upload de arquivos para o Cloudinary.
 * Ajustado para usar a URL de ambiente unificada e evitar erros de injeção.
 */
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Injeta a URL completa do Cloudinary.
     * Formato esperado no application-local.yml:
     * cloudinary://API_KEY:API_SECRET@CLOUD_NAME
     */
    public CloudinaryService(@Value("${cloudinary.url}") String cloudinaryUrl) {
        this.cloudinary = new Cloudinary(cloudinaryUrl);
    }

    /**
     * Faz upload de uma imagem para o Cloudinary de forma reativa.
     */
    public Mono<Map<String, String>> uploadImagem(FilePart file, String folder) {
        return file.content()
                .reduce(new ByteArrayOutputStream(), (baos, dataBuffer) -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        baos.write(bytes);
                    } catch (IOException e) {
                        throw new RuntimeException("Erro ao ler arquivo para upload", e);
                    }
                    return baos;
                })
                .map(ByteArrayOutputStream::toByteArray)
                .flatMap(bytes ->
                        // Schedulers.boundedElastic() garante que o I/O bloqueante do Cloudinary
                        // não trave a thread principal do WebFlux.
                        Mono.fromCallable(() -> {
                            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                                    bytes,
                                    ObjectUtils.asMap(
                                            "folder", folder,
                                            "resource_type", "auto"
                                    )
                            );

                            if (uploadResult == null || uploadResult.get("public_id") == null) {
                                throw new RuntimeException("Falha ao obter resposta do Cloudinary. Verifique suas credenciais.");
                            }

                            // Retornamos chaves consistentes para o ItemAcervoService
                            return Map.of(
                                    "publicId", uploadResult.get("public_id").toString(),
                                    "url", uploadResult.get("secure_url").toString()
                            );
                        }).subscribeOn(Schedulers.boundedElastic())
                );
    }
}