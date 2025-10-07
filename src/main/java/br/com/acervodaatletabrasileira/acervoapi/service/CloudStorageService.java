package br.com.acervodaatletabrasileira.acervoapi.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
public class CloudStorageService {

    // Injeta o nome do bucket do arquivo application.properties
    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    private final Storage storage;

    /**
     * O Spring Cloud GCP injeta automaticamente um objeto 'Storage'
     * autenticado, utilizando a Conta de Serviço do Cloud Run.
     */
    public CloudStorageService(Storage storage) {
        this.storage = storage;
    }

    /**
     * Faz o upload do arquivo para o Google Cloud Storage.
     * * @param filePart O arquivo reativo enviado pelo frontend (FilePart).
     * @return A URL pública do arquivo no GCS.
     */
    public String uploadFile(FilePart filePart) throws IOException {

        // Embora o WebFlux seja reativo, o método de GCS 'storage.create' é blocante.
        // O Mono.fromCallable no Controller garante que essa operação ocorra em um thread separado.

        String originalFilename = filePart.filename();

        // --- 1. Geração de Nome Único ---
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = "atletas_imagens/" + UUID.randomUUID().toString() + extension;

        // --- 2. Criação do Blob ---
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(filePart.headers().getContentType().toString())
                .build();

        // O FilePart precisa ser lido para ser salvo.
        // Usamos o transferTo() para salvar temporariamente e ler, ou lidamos com o InputStream

        // A maneira mais limpa em WebFlux/Spring:
        // Crie um arquivo temporário no sistema e use-o para o upload.
        java.nio.file.Path tempFile = null;
        try {
            tempFile = java.nio.file.Files.createTempFile("upload-", originalFilename);

            // Transfere o conteúdo reativo do FilePart para o arquivo temporário (Operação Blocante)
            Mono<Void> transferMono = filePart.transferTo(tempFile);
            transferMono.block(); // Bloqueia para esperar a escrita antes do upload

            // --- 3. Upload ---
            // Envia o arquivo temporário para o GCS
            storage.createFrom(blobInfo, tempFile);

        } catch (Exception e) {
            throw new IOException("Falha no processamento ou upload do arquivo: " + e.getMessage(), e);
        } finally {
            // Garante que o arquivo temporário seja deletado
            if (tempFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Ignora falha na exclusão do arquivo temporário
                }
            }
        }

        // --- 4. Retorno da URL Pública ---
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
    }
}