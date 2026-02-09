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
     *
     * Retorna:
     * - publicId  → identificador único do asset
     * - version   → versão do asset (imutabilidade e cache)
     * - url       → secure_url (fallback técnico)
     */
    public Mono<Map<String, Object>> uploadImagem(FilePart file, String folder) {
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
                                    "publicId", uploadResult.get("public_id"),
                                    "version", uploadResult.get("version"),
                                    "url", uploadResult.get("secure_url")
                            );
                        }).subscribeOn(Schedulers.boundedElastic())
                );
    }

    /**
     * Remove uma imagem do Cloudinary pelo Public ID.
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

    /* ==========================
       UTIL (Validação de Existência)
       ========================== */

    public Mono<Boolean> resourceExists(String publicId) {
        return Mono.fromCallable(() -> {
                    try {
                        cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /* ==========================
       UTIL (URL PROTEGIDA COM WATERMARK)
       ========================== */

    /**
     * Gera uma URL pública protegida a partir do publicId original.
     * OBS: método utilitário, não deve ser persistido como URL final.
     */
    public String gerarUrlProtegidaComWatermark(String publicId) {
        return cloudinary.url()
                .transformation(
                        new com.cloudinary.Transformation()
                                .width(1200)
                                .crop("limit")
                                .overlay("acervo:watermark_acervo")
                                .gravity("center")
                                .opacity(40)
                                .flags("layer_apply")
                )
                .secure(true)
                .generate(publicId);
    }
}
