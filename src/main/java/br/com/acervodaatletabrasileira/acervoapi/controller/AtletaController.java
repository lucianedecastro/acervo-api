package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import br.com.acervodaatletabrasileira.acervoapi.service.AtletaService;
import br.com.acervodaatletabrasileira.acervoapi.service.CloudStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/atletas")
@Tag(name = "Atletas", description = "Endpoints para gerenciamento de atletas")
public class AtletaController {

    @Autowired
    private AtletaService atletaService;

    @Autowired
    private ObjectMapper objectMapper;

    private final CloudStorageService storageService;

    public AtletaController(CloudStorageService storageService) {
        this.storageService = storageService;
    }

    // --- READ: Listar e Buscar ---
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

    // --- CREATE: Adiciona uma nova atleta ---
    @Operation(summary = "Adiciona uma nova atleta (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Atleta criada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @PostMapping(consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Atleta> createAtleta(
            @RequestPart("file") FilePart filePart,
            @RequestPart("dados") String dadosJson) {

        AtletaFormDTO dto;
        try {
            dto = objectMapper.readValue(dadosJson, AtletaFormDTO.class);
        } catch (JsonProcessingException e) {
            return Mono.error(new IllegalArgumentException("Formato de dados 'dados' inválido. Deve ser JSON.", e));
        }

        Mono<String> imageUrlMono = storageService.uploadFile(filePart)
                .subscribeOn(Schedulers.boundedElastic());

        return imageUrlMono
                .map(imageUrl -> {
                    // 🆕 FOTO COM CONTROLE DE DESTAQUE
                    FotoAcervo novaFoto = new FotoAcervo(imageUrl, dto.legenda());
                    novaFoto.setEhDestaque(true); // 🎯 Primeira foto é sempre destaque

                    Atleta novaAtleta = new Atleta();
                    novaAtleta.setNome(dto.nome());
                    novaAtleta.setModalidade(dto.modalidade());
                    novaAtleta.setBiografia(dto.biografia());
                    novaAtleta.setCompeticao(dto.competicao());

                    List<FotoAcervo> fotos = new ArrayList<>();
                    fotos.add(novaFoto);
                    novaAtleta.setFotos(fotos);
                    novaAtleta.setFotoDestaqueId(novaFoto.getId()); // 🎯 Define destaque

                    return novaAtleta;
                })
                .flatMap(atletaService::save);
    }

    // --- UPDATE: Atualiza informações de uma atleta ---
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
            @RequestPart("dados") String dadosJson) {

        AtletaFormDTO dto;
        try {
            dto = objectMapper.readValue(dadosJson, AtletaFormDTO.class);
        } catch (JsonProcessingException e) {
            return Mono.error(new IllegalArgumentException("Formato de dados 'dados' inválido. Deve ser JSON.", e));
        }

        return atletaService.findById(id)
                .flatMap(atletaExistente -> {
                    // Upload opcional da nova imagem
                    Mono<String> newImageUrlMono = filePartMono
                            .flatMap(filePart ->
                                    storageService.uploadFile(filePart)
                                            .subscribeOn(Schedulers.boundedElastic())
                            )
                            .switchIfEmpty(Mono.justOrEmpty(
                                    Optional.ofNullable(atletaExistente.getFotos())
                                            .filter(f -> !f.isEmpty())
                                            .map(f -> f.get(0).getUrl())
                            ));

                    return newImageUrlMono.flatMap(finalImageUrl -> {
                        // Atualiza os campos básicos
                        atletaExistente.setNome(dto.nome());
                        atletaExistente.setModalidade(dto.modalidade());
                        atletaExistente.setBiografia(dto.biografia());
                        atletaExistente.setCompeticao(dto.competicao());

                        // 🆕 ATUALIZAÇÃO INTELIGENTE DAS FOTOS
                        List<FotoAcervo> fotos = Optional.ofNullable(atletaExistente.getFotos()).orElseGet(ArrayList::new);

                        if (finalImageUrl != null && finalImageUrl.contains("storage.googleapis.com")) {
                            FotoAcervo novaFoto = new FotoAcervo(finalImageUrl, dto.legenda());

                            // 🎯 ESTRATÉGIA: Se não tem fotos, nova é destaque. Se já tem, é adicional.
                            if (fotos.isEmpty()) {
                                novaFoto.setEhDestaque(true);
                                atletaExistente.setFotoDestaqueId(novaFoto.getId());
                            } else {
                                novaFoto.setEhDestaque(false);
                            }

                            fotos.add(novaFoto);
                            atletaExistente.setFotos(fotos);
                        }

                        return atletaService.update(id, atletaExistente)
                                .map(ResponseEntity::ok);
                    });
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // --- DELETE: Remove uma atleta ---
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