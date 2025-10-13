package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ModalidadeDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import br.com.acervodaatletabrasileira.acervoapi.service.CloudStorageService;
import br.com.acervodaatletabrasileira.acervoapi.service.ModalidadeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/modalidades")
@Tag(name = "Modalidades", description = "Endpoints para gerenciamento de modalidades")
public class ModalidadeController {

    @Autowired
    private ModalidadeService modalidadeService;

    @Autowired
    private CloudStorageService storageService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- READ ---
    @Operation(summary = "Lista todas as modalidades")
    @GetMapping
    public Flux<Modalidade> getAllModalidades() {
        return modalidadeService.findAll();
    }

    @Operation(summary = "Busca uma modalidade pelo ID")
    @GetMapping("/{id}")
    // ✅ CORREÇÃO: Especifica o nome do path variable para máxima clareza.
    public Mono<ResponseEntity<Modalidade>> getModalidadeById(@PathVariable("id") String id) {
        return modalidadeService.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // --- CREATE ---
    @Operation(summary = "Cria uma nova modalidade (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Modalidade> createModalidade(
            @RequestPart(value = "file", required = false) Mono<FilePart> filePartMono,
            @RequestPart("dados") String dadosJson) {

        ModalidadeDTO dto;
        try {
            dto = objectMapper.readValue(dadosJson, ModalidadeDTO.class);
        } catch (JsonProcessingException e) {
            return Mono.error(new IllegalArgumentException("Formato de 'dados' inválido.", e));
        }

        return filePartMono
                .flatMap(storageService::uploadFile)
                .defaultIfEmpty("")
                .flatMap(pictogramaUrl -> {
                    Modalidade novaModalidade = new Modalidade();
                    novaModalidade.setNome(dto.nome());
                    novaModalidade.setHistoria(dto.historia());
                    if (!pictogramaUrl.isBlank()) {
                        novaModalidade.setPictogramaUrl(pictogramaUrl);
                    }
                    return modalidadeService.save(novaModalidade);
                });
    }

    // --- UPDATE ---
    @Operation(summary = "Atualiza uma modalidade (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public Mono<ResponseEntity<Modalidade>> updateModalidade(
            @PathVariable("id") String id,
            @RequestPart(value = "file", required = false) Mono<FilePart> filePartMono,
            @RequestPart("dados") String dadosJson) {

        // 1. Desserializa o JSON para DTO
        ModalidadeDTO dto;
        try {
            dto = objectMapper.readValue(dadosJson, ModalidadeDTO.class);
        } catch (JsonProcessingException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

                Mono<String> uploadMono = filePartMono
                .flatMap(storageService::uploadFile)
                .defaultIfEmpty(null);

        // 3. Chama o service para atualizar a modalidade
        return uploadMono.flatMap(pictogramaUrl ->
                modalidadeService.update(id, dto, pictogramaUrl)
                        .map(ResponseEntity::ok)
        ).defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // --- DELETE ---
    @Operation(summary = "Deleta uma modalidade (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // ✅ CORREÇÃO: Especifica o nome do path variable.
    public Mono<Void> deleteModalidade(@PathVariable("id") String id) {
        return modalidadeService.deleteById(id);
    }
}