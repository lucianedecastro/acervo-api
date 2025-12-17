package br.com.acervodaatletabrasileira.acervoapi.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Serviço responsável pelo upload de arquivos para o Cloudinary.
 *
 * Trabalha de forma reativa com WebFlux (FilePart).
 * Retorna apenas os dados essenciais para persistência no acervo.
 */
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Faz upload de uma imagem para o Cloudinary.
     *
     * @param file arquivo recebido via multipart
     * @param folder pasta lógica no Cloudinary (ex: acervo/atletas)
     * @return Mono com Map contendo publicId e url
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
                        Mono.fromCallable(() -> {

                            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                                    bytes,
                                    ObjectUtils.asMap(
                                            "folder", folder,
                                            "resource_type", "image"
                                    )
                            );

                            return Map.of(
                                    "publicId", uploadResult.get("public_id").toString(),
                                    "url", uploadResult.get("secure_url").toString()
                            );
                        })
                );
    }
}
