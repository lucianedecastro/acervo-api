package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ImportService {

    private final Firestore firestore;
    private static final int BATCH_SIZE = 500;

    @Autowired
    public ImportService(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Processa o upload do arquivo CSV e salva os atletas no Firestore em lote.
     */
    public Mono<Void> processCsvUpload(MultipartFile file) {
        return Mono.fromRunnable(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

                String line;
                boolean isHeader = true;
                int count = 0;

                WriteBatch batch = firestore.batch();

                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }

                    Atleta atleta = mapCsvLineToAtleta(line);

                    if (atleta != null) {
                        // O Firestore gera o ID automaticamente
                        batch.set(firestore.collection("atletas").document(UUID.randomUUID().toString()), atleta);
                        count++;

                        if (count % BATCH_SIZE == 0) {
                            batch.commit().get();
                            batch = firestore.batch();
                        }
                    }
                }

                // Comita o último lote restante
                if (count % BATCH_SIZE != 0) {
                    batch.commit().get();
                }

            } catch (Exception e) {
                throw new RuntimeException("Erro ao processar o CSV: " + e.getMessage(), e);
            }
        }).then();
    }

    /**
     * Mapeia uma linha CSV para um objeto Atleta.
     * Tenta identificar o delimitador (vírgula ou ponto e vírgula) para maior robustez.
     */
    private Atleta mapCsvLineToAtleta(String csvLine) {

        // 1. Determina o delimitador: Ponto e Vírgula tem prioridade (padrão Brasil)
        String delimiter = csvLine.contains(";") ? ";" : ",";

        // 2. Faz o split
        String[] fields = csvLine.split(delimiter);

        // Garante que a linha tenha o número correto de campos (5)
        // Isso é crucial para o erro de vírgulas internas que resolvemos.
        if (fields.length < 5) return null;

        Atleta atleta = new Atleta();
        atleta.setNome(fields[0].trim());
        atleta.setModalidade(fields[1].trim());
        atleta.setBiografia(fields[2].trim());
        atleta.setCompeticao(fields[3].trim());

        // O campo 5 é a imagemUrl
        String imageUrl = fields[4].trim();

        // Adiciona a foto (mesmo que a URL esteja vazia)
        if (!imageUrl.isEmpty()) {
            // Cria o objeto FotoAcervo com a URL e uma legenda padrão (vazia)
            atleta.setFotos(List.of(new FotoAcervo(imageUrl, "")));
        } else {
            atleta.setFotos(new ArrayList<>());
        }

        return atleta;
    }
}