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
 * Serviço responsável pelo upload e gerenciamento de arquivos no Cloudinary.
 * Integrado ao fluxo reativo (WebFlux) do Acervo da Atleta Brasileira.
 */
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(@Value("${cloudinary.url}") String cloudinaryUrl) {
        this.cloudinary = new Cloudinary(cloudinaryUrl);
    }

    /**
     * Faz upload de uma imagem para o Cloudinary de forma reativa.
     * Retorna o publicId e a secureUrl (HTTPS).
     */
    public Mono<Map<String, String>> uploadImagem(FilePart file, String folder) {
        return file.content()
                .reduce(new ByteArrayOutputStream(), (baos, dataBuffer) -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        baos.write(bytes);
                    } catch (IOException e) {
                        throw new RuntimeException("Erro ao processar o fluxo de bytes do arquivo", e);
                    }
                    return baos;
                })
                .map(ByteArrayOutputStream::toByteArray)
                .flatMap(bytes ->
                        Mono.fromCallable(() -> {
                            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                                    bytes,
                                    ObjectUtils.asMap(
                                            "folder", "acervo/" + folder,
                                            "resource_type", "auto"
                                    )
                            );

                            if (uploadResult == null || uploadResult.get("public_id") == null) {
                                throw new RuntimeException("Falha na comunicação com Cloudinary. Verifique as chaves.");
                            }

                            return Map.of(
                                    "publicId", uploadResult.get("public_id").toString(),
                                    "url", uploadResult.get("secure_url").toString()
                            );
                        }).subscribeOn(Schedulers.boundedElastic())
                );
    }

    /**
     * Remove uma imagem do Cloudinary pelo Public ID.
     * Importante para manter o armazenamento limpo ao deletar itens.
     */
    public Mono<Void> deleteImagem(String publicId) {
        return Mono.<Void>fromRunnable(() -> {
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (IOException e) {
                throw new RuntimeException("Erro ao deletar imagem no Cloudinary", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}