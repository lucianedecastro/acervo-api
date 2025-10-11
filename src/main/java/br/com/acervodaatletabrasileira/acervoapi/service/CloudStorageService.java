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

@Service
public class CloudStorageService {

    private static final Logger logger = LoggerFactory.getLogger(CloudStorageService.class);

    @Value("${gcp.storage.bucket-name:}")
    private String bucketName;

    private final Storage storage;

    public CloudStorageService(Storage storage) {
        this.storage = storage;
    }

    public Mono<String> uploadFile(FilePart filePart) {
        return Mono.fromCallable(() -> {
                    if (bucketName == null || bucketName.isBlank()) {
                        throw new IllegalStateException("Bucket name não configurado. Verifique a variável 'gcp.storage.bucket-name'.");
                    }

                    String originalFilename = Optional.ofNullable(filePart.filename()).orElse("sem_nome");
                    String extension = originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
                    String fileName = "atletas_imagens/" + UUID.randomUUID() + extension;
                    String contentType = getContentTypeFromFilename(originalFilename);

                    BlobId blobId = BlobId.of(bucketName, fileName);
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

                    Path tempFile = Files.createTempFile("upload-", originalFilename);

                    try {
                        filePart.transferTo(tempFile).block();
                        storage.createFrom(blobInfo, tempFile);
                        logger.info("Upload concluído: {}", fileName);
                        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
                    } finally {
                        try {
                            Files.deleteIfExists(tempFile);
                        } catch (IOException ex) {
                            logger.warn("Falha ao excluir arquivo temporário: {}", tempFile);
                        }
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> new IOException("Erro ao enviar arquivo para o bucket " + bucketName, e));
    }

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

    // ✅ NOVO MÉTODO ADICIONADO
    /**
     * Deleta um arquivo do bucket do GCS com base em sua URL completa.
     * @param fileUrl A URL pública do arquivo a ser deletado.
     * @return Mono<Void> que completa quando a deleção é concluída.
     */
    public Mono<Void> deleteFile(String fileUrl) {
        return Mono.fromRunnable(() -> {
            try {
                if (fileUrl == null || !fileUrl.contains(bucketName)) {
                    logger.warn("URL de arquivo inválida para deleção: {}", fileUrl);
                    return;
                }
                String objectName = fileUrl.substring(fileUrl.indexOf(bucketName) + bucketName.length() + 1);
                BlobId blobId = BlobId.of(bucketName, objectName);

                boolean deleted = storage.delete(blobId);

                if (deleted) {
                    logger.info("Arquivo órfão deletado com sucesso: {}", objectName);
                } else {
                    logger.warn("Arquivo órfão não encontrado para deleção: {}", objectName);
                }
            } catch (Exception e) {
                logger.error("Erro ao tentar deletar arquivo órfão: " + fileUrl, e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}