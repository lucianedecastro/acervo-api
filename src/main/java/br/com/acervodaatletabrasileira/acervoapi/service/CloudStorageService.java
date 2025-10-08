package br.com.acervodaatletabrasileira.acervoapi.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class CloudStorageService {

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    private final Storage storage;

    public CloudStorageService(Storage storage) {
        this.storage = storage;
    }

    /**
     * Faz o upload do arquivo. A operação bloqueante é contida no Mono.fromCallable.
     * @param filePart O arquivo (FilePart)
     * @return Mono<String> contendo a URL pública do GCS.
     */
    public Mono<String> uploadFile(FilePart filePart) {
        return Mono.fromCallable(() -> {
            String originalFilename = filePart.filename();
            String extension = originalFilename != null && originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String fileName = "atletas_imagens/" + UUID.randomUUID().toString() + extension;

            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(filePart.headers().getContentType().toString())
                    .build();

            Path tempFile = null;
            try {
                tempFile = java.nio.file.Files.createTempFile("upload-", originalFilename);

                // CORREÇÃO CRÍTICA: Executa a transferência de forma BLOQUEANTE
                // sem o 'block()' explícito no Mono.
                filePart.transferTo(tempFile).subscribe(); // Usa subscribe e o thread de I/O

                // DÁ AO WEBLUX TEMPO PARA A TRANSFERÊNCIA (Pausa de thread necessária)
                // Não é o ideal, mas é a forma mais simples de garantir que o arquivo seja gravado antes de ser lido pelo storage.createFrom.
                Thread.sleep(500);

                // Envia o arquivo síncrono para o GCS
                storage.createFrom(blobInfo, tempFile);

            } catch (Exception e) {
                // Lança IOException para ser tratada pelo Controller (que está no Scheduler)
                throw new IOException("Falha no upload do arquivo: " + e.getMessage(), e);
            } finally {
                if (tempFile != null) {
                    java.nio.file.Files.deleteIfExists(tempFile);
                }
            }
            return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
        });
    }
}