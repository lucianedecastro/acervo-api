package br.com.acervodaatletabrasileira.acervoapi.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço especializado na manipulação de documentos legais e sensíveis.
 * Responsável por garantir a integridade (Hashing) antes do armazenamento.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentoLegalService {

    private final Cloudinary cloudinary;

    /**
     * Processa o upload de um Contrato de Gestão ou Documento de Identidade.
     * * Fluxo:
     * 1. Recebe o arquivo (FilePart) de forma reativa.
     * 2. Salva em disco temporário para permitir leitura dupla.
     * 3. Calcula o Hash SHA-256 (para prova na Blockchain).
     * 4. Faz o upload para o Cloudinary (pasta protegida).
     * 5. Remove o arquivo temporário.
     * 6. Retorna os metadados (URL e Hash).
     */
    public Mono<MetadataDocumentoLegal> processarDocumentoLegal(FilePart filePart, String atletaId, TipoDocumento tipo) {
        return Mono.fromCallable(() -> {
                    // Cria um arquivo temporário seguro
                    Path tempFile = Files.createTempFile("doc_" + atletaId, ".tmp");
                    return tempFile;
                })
                .flatMap(path -> filePart.transferTo(path) // Transfere o stream para o arquivo temp
                        .then(Mono.fromCallable(() -> {
                            File file = path.toFile();

                            // 1. Calcular o Hash SHA-256 (A Prova de Integridade)
                            String hash = calcularHashSHA256(file);
                            log.info("Hash gerado para documento {}: {}", tipo, hash);

                            // 2. Definir pasta de destino baseada no tipo
                            String folder = tipo == TipoDocumento.CONTRATO ? "acervo/contratos" : "acervo/identidade";
                            String publicId = folder + "/" + atletaId + "/" + UUID.randomUUID();

                            // 3. Upload para Cloudinary (como 'raw' ou 'auto' para PDFs)
                            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                                    "public_id", publicId,
                                    "resource_type", "auto", // Detecta se é PDF ou Imagem
                                    "access_mode", "public" // Pode ser mudado para 'authenticated' para maior restrição
                            ));

                            // 4. Montar objeto de retorno
                            String url = (String) uploadResult.get("secure_url");

                            return new MetadataDocumentoLegal(url, hash, publicId);

                        }).subscribeOn(Schedulers.boundedElastic())) // Executa bloqueante em thread separada
                        .doFinally(signal -> {
                            // Limpeza: Deleta o arquivo temporário após o processo
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.error("Erro ao deletar arquivo temporário: {}", path, e);
                            }
                        })
                );
    }

    /**
     * Método utilitário para gerar o SHA-256 de um arquivo físico.
     */
    private String calcularHashSHA256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * DTO interno para transportar os dados processados.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class MetadataDocumentoLegal {
        private String url;
        private String hashIntegridade;
        private String publicId;
    }

    public enum TipoDocumento {
        CONTRATO,
        IDENTIDADE_ATLETA,
        PROVA_HISTORICA
    }
}
