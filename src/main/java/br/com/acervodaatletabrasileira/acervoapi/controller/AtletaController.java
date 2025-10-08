package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import br.com.acervodaatletabrasileira.acervoapi.service.AtletaService;
import br.com.acervodaatletabrasileira.acervoapi.service.CloudStorageService;
import com.fasterxml.jackson.databind.ObjectMapper; // NOVO IMPORT
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/atletas")
@Tag(name = "Atletas", description = "Endpoints para gerenciamento de atletas")
public class AtletaController {

    @Autowired
    private AtletaService atletaService;

    // NOVO: ObjectMapper para converter a String JSON em DTO
    @Autowired
    private ObjectMapper objectMapper;

    private final CloudStorageService storageService;

    public AtletaController(CloudStorageService storageService) {
        this.storageService = storageService;
    }

    // --- READ: Listar e Buscar (Não Alterado) ---
    // ... (Métodos getAllAtletas e getAtletaById permanecem inalterados) ...
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

    // --- CREATE: Adiciona uma nova atleta (CORREÇÃO DE DESSERIALIZAÇÃO) ---
    @Operation(summary = "Adiciona uma nova atleta (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Atleta criada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @PostMapping(consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Atleta> createAtleta(
            @RequestPart("file") FilePart filePart,
            // CORREÇÃO: Recebe como String e desserializa manualmente
            @RequestPart("dados") String dadosJson) throws IOException {

        // 1. DESSERIALIZAÇÃO MANUAL: Converte a String JSON em DTO
        AtletaFormDTO dto = objectMapper.readValue(dadosJson, AtletaFormDTO.class);

        // 2. Faz o upload da imagem e obtém a URL (em Mono, para ser reativo)
        Mono<String> imageUrlMono = Mono.fromCallable(() -> {
            try {
                return storageService.uploadFile(filePart);
            } catch (IOException e) {
                throw new RuntimeException("Falha no upload do arquivo para o GCS", e);
            }
        });

        // 3. Encadear a operação: upload -> montar modelo -> salvar no Firestore
        return imageUrlMono
                .map(imageUrl -> {
                    // Monta o objeto FotoAcervo
                    FotoAcervo novaFoto = new FotoAcervo(imageUrl, dto.legenda());

                    // Monta o objeto Atleta
                    Atleta novaAtleta = new Atleta();
                    novaAtleta.setNome(dto.nome());
                    novaAtleta.setModalidade(dto.modalidade());
                    novaAtleta.setBiografia(dto.biografia());
                    novaAtleta.setCompeticao(dto.competicao());

                    // Adiciona a nova foto à galeria
                    List<FotoAcervo> fotos = new ArrayList<>();
                    fotos.add(novaFoto);
                    novaAtleta.setFotos(fotos);

                    return novaAtleta;
                })
                .flatMap(atletaService::save);
    }

    // --- UPDATE: Atualiza informações de uma atleta (CORREÇÃO DE DESSERIALIZAÇÃO) ---
    @Operation(summary = "Atualiza informações de uma atleta (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Atleta atualizada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado"),
            @ApiResponse(responseCode = "404", description = "Atleta não encontrada")
    })
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public Mono<ResponseEntity<Atleta>> updateAtleta(
            @PathVariable("id") String id,
            @RequestPart(value = "file", required = false) Mono<FilePart> filePartMono,
            // CORREÇÃO: Recebe como String e desserializa manualmente
            @RequestPart("dados") String dadosJson) throws IOException {

        // 1. DESSERIALIZAÇÃO MANUAL: Converte a String JSON em DTO
        AtletaFormDTO dto = objectMapper.readValue(dadosJson, AtletaFormDTO.class);

        // 2. Buscar a atleta existente
        return atletaService.findById(id)
                .flatMap(atletaExistente -> {
                    // 3. Processar o upload (opcional)
                    Mono<String> newImageUrlMono = filePartMono
                            .flatMap(filePart -> {
                                return Mono.fromCallable(() -> {
                                    try {
                                        return storageService.uploadFile(filePart);
                                    } catch (IOException e) {
                                        throw new RuntimeException("Falha no upload do arquivo para o GCS", e);
                                    }
                                });
                            })
                            // Se nenhum arquivo for enviado, usa a URL existente
                            .defaultIfEmpty(
                                    atletaExistente.getFotos() != null && !atletaExistente.getFotos().isEmpty() ?
                                            atletaExistente.getFotos().get(0).getUrl() : null
                            );

                    // 4. Encadear: obter URL -> atualizar metadados -> salvar
                    return newImageUrlMono.flatMap(finalImageUrl -> {
                        // Copiar metadados do DTO
                        atletaExistente.setNome(dto.nome());
                        atletaExistente.setModalidade(dto.modalidade());
                        atletaExistente.setBiografia(dto.biografia());
                        atletaExistente.setCompeticao(dto.competicao());

                        // 5. Atualizar a lista de fotos (lógica de anexo permanece)
                        List<FotoAcervo> fotos = Optional.ofNullable(atletaExistente.getFotos()).orElseGet(ArrayList::new);

                        if (finalImageUrl != null && !finalImageUrl.isEmpty()) {
                            FotoAcervo novaFoto = new FotoAcervo(finalImageUrl, dto.legenda());
                            fotos.add(novaFoto);
                        }
                        atletaExistente.setFotos(fotos);

                        return atletaService.update(id, atletaExistente)
                                .map(ResponseEntity::ok);
                    });
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // --- DELETE: Remove uma atleta do banco de dados (Não Alterado) ---
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