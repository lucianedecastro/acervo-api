package br.com.acervodaatletabrasileira.acervoapi.controller;

import org.springframework.beans.factory.annotation.Autowired;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.service.AtletaService;
import br.com.acervodaatletabrasileira.acervoapi.service.CloudStorageService; // NOVO IMPORT
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart; // NOVO IMPORT PARA WEBSOCKETS
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/atletas")
@Tag(name = "Atletas", description = "Endpoints para gerenciamento de atletas")
public class AtletaController {

    // Mantido como @Autowired para evitar alterações estruturais no arquivo original
    @Autowired
    private AtletaService atletaService;

    // NOVO: Adiciona o serviço de Storage. Injeção será feita via Setter ou Field.
    // **NOTA:** Em WebFlux, é mais seguro injetar via construtor, mas mantive o estilo @Autowired do seu código.
    private final CloudStorageService storageService;

    // Construtor para injeção (WebFlux prefere isso ou o @Autowired no campo)
    public AtletaController(CloudStorageService storageService) {
        this.storageService = storageService;
    }

    // --- MÉTODOS EXISTENTES (GET, DELETE) NÃO ALTERADOS ---

    @Operation(summary = "Lista todas as atletas cadastradas")
    @GetMapping
    public Flux<Atleta> getAllAtletas() {
        return atletaService.findAll();
    }

    @Operation(summary = "Busca uma atleta pelo seu ID único")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atleta encontrada"),
            @ApiResponse(responseCode = "404", description = "Atleta não encontrada")
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Atleta>> getAtletaById(@PathVariable("id") String id) {
        return atletaService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // --- CREATE: Adiciona uma nova atleta (Alterado para lidar com o arquivo) ---
    @Operation(summary = "Adiciona uma nova atleta (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Atleta criada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @PostMapping(consumes = {"multipart/form-data"}) // Consome FormData
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Atleta> createAtleta(
            // Recebe o arquivo (com o nome 'file' do frontend)
            @RequestPart("file") FilePart filePart,
            // Recebe os demais dados como um mapa (melhor que RequestBody com RequestPart)
            @RequestPart("atleta") Atleta atletaData) {

        // 1. Faz o upload da imagem de forma reativa e obtém a URL
        Mono<String> imageUrlMono = Mono.fromCallable(() -> {
            try {
                // Chama o serviço de upload. Converte o FilePart em um recurso que o GCS pode ler.
                return storageService.uploadFile(filePart);
            } catch (IOException e) {
                // Caso a leitura/upload falhe
                throw new RuntimeException("Falha no upload do arquivo para o GCS", e);
            }
        });

        // 2. Encadear a operação: upload -> set URL -> salvar no Firestore
        return imageUrlMono
                .doOnNext(imageUrl -> atletaData.setImagemUrl(imageUrl)) // Define a URL na entidade
                .flatMap(imageUrl -> atletaService.save(atletaData)); // Salva no Firestore
    }

    // --- UPDATE: Atualiza informações de uma atleta (Alterado para lidar com o arquivo) ---
    @Operation(summary = "Atualiza informações de uma atleta (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atleta atualizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Atleta não encontrada")
    })
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"}) // Consome FormData
    public Mono<ResponseEntity<Atleta>> updateAtleta(
            @PathVariable("id") String id,
            @RequestPart(value = "file", required = false) Mono<FilePart> filePartMono,
            @RequestPart("atleta") Atleta atletaData) {

        // 1. Buscar a atleta existente
        return atletaService.findById(id)
                .flatMap(atletaExistente -> {
                    // 2. Processar a parte do arquivo (Upload opcional)
                    Mono<String> newImageUrlMono = filePartMono
                            .flatMap(filePart -> {
                                // Se um arquivo foi enviado, faz o upload e retorna a nova URL
                                return Mono.fromCallable(() -> {
                                    try {
                                        return storageService.uploadFile(filePart);
                                    } catch (IOException e) {
                                        throw new RuntimeException("Falha no upload do arquivo para o GCS", e);
                                    }
                                });
                            })
                            // Se nenhum arquivo for enviado, usa a URL existente
                            .defaultIfEmpty(atletaExistente.getImagemUrl());

                    // 3. Encadear: obter URL -> atualizar metadados -> salvar
                    return newImageUrlMono.flatMap(finalImageUrl -> {
                        // Copiar e atualizar dados
                        atletaExistente.setNome(atletaData.getNome());
                        atletaExistente.setModalidade(atletaData.getModalidade());
                        // ... copiar outros campos de atletaData para atletaExistente
                        atletaExistente.setImagemUrl(finalImageUrl); // Atualiza com a nova ou antiga URL

                        return atletaService.update(id, atletaExistente)
                                .map(ResponseEntity::ok);
                    });
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Remove uma atleta do banco de dados (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Atleta deletada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @DeleteMapping("/{id}")
    public Mono<Void> deleteAtleta(@PathVariable("id") String id) {
        return atletaService.deleteById(id);
    }
}