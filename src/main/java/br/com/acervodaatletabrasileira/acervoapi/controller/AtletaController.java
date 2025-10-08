package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import br.com.acervodaatletabrasileira.acervoapi.service.AtletaService;
import br.com.acervodaatletabrasileira.acervoapi.service.CloudStorageService;
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

    // Injeção via campo (mantido para evitar alterações estruturais)
    @Autowired
    private AtletaService atletaService;

    private final CloudStorageService storageService;

    // Construtor com injeção (mantido)
    public AtletaController(CloudStorageService storageService) {
        this.storageService = storageService;
    }

    // --- READ: Listar todas as atletas cadastradas ---
    @Operation(summary = "Lista todas as atletas cadastradas")
    @GetMapping
    public Flux<Atleta> getAllAtletas() {
        return atletaService.findAll();
    }

    // --- READ: Busca uma atleta pelo seu ID único ---
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

    // --- CREATE: Adiciona uma nova atleta (CORRIGIDO) ---
    // Recebe o arquivo e os dados do formulário
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
            // Recebe os demais dados do formulário (JSON)
            @RequestPart("dados") AtletaFormDTO dto) { // MUDANÇA: Usa AtletaFormDTO

        // 1. Faz o upload da imagem e obtém a URL (em Mono, para ser reativo)
        Mono<String> imageUrlMono = Mono.fromCallable(() -> {
            try {
                return storageService.uploadFile(filePart);
            } catch (IOException e) {
                // Em caso de falha de upload, lançamos uma exceção
                throw new RuntimeException("Falha no upload do arquivo para o GCS", e);
            }
        });

        // 2. Encadear a operação: upload -> montar modelo -> salvar no Firestore
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
                    novaAtleta.setFotos(fotos); // Usa a lista de fotos

                    return novaAtleta;
                })
                .flatMap(atletaService::save); // 3. Salva no Firestore
    }

    // --- UPDATE: Atualiza informações de uma atleta (CORRIGIDO) ---
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
            @RequestPart("dados") AtletaFormDTO dto) { // MUDANÇA: Usa AtletaFormDTO

        // 1. Buscar a atleta existente
        return atletaService.findById(id)
                .flatMap(atletaExistente -> {
                    // 2. Processar o upload (opcional)
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
                            // Se nenhum arquivo for enviado, usa a URL existente (a URL da última foto, se existir)
                            .defaultIfEmpty(
                                    atletaExistente.getFotos() != null && !atletaExistente.getFotos().isEmpty() ?
                                            atletaExistente.getFotos().get(0).getUrl() : null
                            );

                    // 3. Encadear: obter URL -> atualizar metadados -> salvar
                    return newImageUrlMono.flatMap(finalImageUrl -> {
                        // Copiar metadados
                        atletaExistente.setNome(dto.nome());
                        atletaExistente.setModalidade(dto.modalidade());
                        atletaExistente.setBiografia(dto.biografia());
                        atletaExistente.setCompeticao(dto.competicao());

                        // 4. Atualizar a lista de fotos
                        List<FotoAcervo> fotos = Optional.ofNullable(atletaExistente.getFotos()).orElseGet(ArrayList::new);

                        if (finalImageUrl != null && !finalImageUrl.isEmpty()) {
                            // Se há uma nova imagem, adicionamos ela ou substituímos a primeira, dependendo da regra de negócios.
                            // Por simplicidade, vamos adicionar a nova imagem.
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

    // --- DELETE: Remove uma atleta do banco de dados ---
    @Operation(summary = "Remove uma atleta do banco de dados (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Atleta deletada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autorizado")
    })
    @DeleteMapping("/{id}")
    public Mono<Void> deleteAtleta(@PathVariable("id") String id) {
        // Lógica de exclusão da foto do GCS deve ser adicionada aqui para a segurança
        return atletaService.deleteById(id);
    }
}