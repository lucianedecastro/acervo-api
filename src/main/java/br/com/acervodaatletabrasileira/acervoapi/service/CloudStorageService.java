package br.com.acervodaatletabrasileira.acervoapi.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço responsável por enviar arquivos ao Google Cloud Storage (GCS).
 * A operação de upload é executada em uma thread de I/O isolada,
 * garantindo compatibilidade com o modelo reativo (WebFlux).
 */
@Service
public class CloudStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CloudStorageService.class);

    @Value("${gcp.storage.bucket-name:}") // ✅ evita NPE se variável não estiver setada
    private String bucketName;

    private final Storage storage;

    public CloudStorageService(Storage storage) {
        this.storage = storage;
    }

    /**
     * Realiza o upload do arquivo para o bucket configurado no GCS.
     *
     * @param filePart arquivo recebido (via multipart)
     * @return Mono<String> contendo a URL pública do arquivo enviado
     */
    public Mono<String> uploadFile(FilePart filePart) {
        return Mono.fromCallable(() -> {
                    // Validação defensiva
                    if (bucketName == null || bucketName.isBlank()) {
                        throw new IllegalStateException(
                                "Bucket name não configurado. Verifique a variável 'gcp.storage.bucket-name' no Cloud Run."
                        );
                    }

                    String originalFilename = Optional.ofNullable(filePart.filename()).orElse("sem_nome");
                    String extension = originalFilename.contains(".")
                            ? originalFilename.substring(originalFilename.lastIndexOf("."))
                            : "";

                    String fileName = "atletas_imagens/" + UUID.randomUUID() + extension;

                    // 🎯 CORREÇÃO DEFINITIVA: EVITA COMPLETAMENTE OS HEADERS
                    String contentType = getContentTypeFromFilename(originalFilename);

                    BlobId blobId = BlobId.of(bucketName, fileName);
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                            .setContentType(contentType)
                            .build();

                    // Processamento direto sem cadeia reativa complexa
                    Path tempFile = Files.createTempFile("upload-", originalFilename);

                    try {
                        // Transferência síncrona do arquivo (em thread isolada)
                        filePart.transferTo(tempFile).block();

                        // Upload para GCS
                        storage.createFrom(blobInfo, tempFile);
                        logger.info("Upload concluído: {}", fileName);

                        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);

                    } finally {
                        // Limpeza garantida do arquivo temporário
                        try {
                            Files.deleteIfExists(tempFile);
                            logger.debug("Arquivo temporário removido: {}", tempFile);
                        } catch (IOException ex) {
                            logger.warn("Falha ao excluir arquivo temporário: {}", tempFile);
                        }
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> new IOException("Erro ao enviar arquivo para o bucket " + bucketName, e));
    }

    // 🎯 MÉTODO AUXILIAR: Determina content type pela extensão do arquivo
    private String getContentTypeFromFilename(String filename) {
        if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (filename.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else {
            return "application/octet-stream";
        }
    }
}