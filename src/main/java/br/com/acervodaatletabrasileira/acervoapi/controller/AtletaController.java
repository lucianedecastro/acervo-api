package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.service.AtletaService;
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

@RestController
@RequestMapping("/atletas")
@Tag(name = "Atletas", description = "Endpoints para gerenciamento de atletas")
public class AtletaController {

    @Autowired
    private AtletaService atletaService;

    @Autowired
    private ObjectMapper objectMapper;

    // --- READ & DELETE (sem alterações) ---

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

    @Operation(summary = "Remove uma atleta do banco de dados (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAtleta(@PathVariable("id") String id) {
        return atletaService.deleteById(id);
    }


    // --- CREATE & UPDATE (simplificados para galeria múltipla) ---

    @Operation(summary = "Adiciona uma nova atleta com galeria de fotos (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Atleta> createAtletaWithGallery(
            // ✅ Recebe múltiplos arquivos, não apenas um
            @RequestPart("files") Flux<FilePart> filePartFlux,
            @RequestPart("dados") String dadosJson) {

        AtletaFormDTO dto;
        try {
            // Converte o JSON de dados para nosso novo DTO
            dto = objectMapper.readValue(dadosJson, AtletaFormDTO.class);
        } catch (JsonProcessingException e) {
            return Mono.error(new IllegalArgumentException("Formato de 'dados' inválido. Deve ser JSON.", e));
        }

        // ✅ Delega toda a lógica complexa para o serviço
        return atletaService.createAtletaWithGallery(dto, filePartFlux);
    }

    @Operation(summary = "Atualiza uma atleta e sua galeria de fotos (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public Mono<ResponseEntity<Atleta>> updateAtletaWithGallery(
            @PathVariable("id") String id,
            // ✅ Recebe múltiplos novos arquivos (opcional)
            @RequestPart(value = "files", required = false) Flux<FilePart> filePartFlux,
            @RequestPart("dados") String dadosJson) {

        AtletaFormDTO dto;
        try {
            dto = objectMapper.readValue(dadosJson, AtletaFormDTO.class);
        } catch (JsonProcessingException e) {
            // Retorna um erro 400 (Bad Request) se o JSON for inválido
            return Mono.just(ResponseEntity.badRequest().build());
        }

        // ✅ Delega a lógica de atualização para o serviço
        return atletaService.updateAtletaWithGallery(id, dto, filePartFlux)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}