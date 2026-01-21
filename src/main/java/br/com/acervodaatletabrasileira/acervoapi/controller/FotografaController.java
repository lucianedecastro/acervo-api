package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotografaPerfilDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.FotografaPublicoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Fotografa;
import br.com.acervodaatletabrasileira.acervoapi.service.FotografaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequestMapping("/fotografas")
@Tag(name = "Fotógrafas", description = "Endpoints públicos, privados e administrativos do acervo de fotógrafas")
public class FotografaController {

    private final FotografaService fotografaService;

    public FotografaController(FotografaService fotografaService) {
        this.fotografaService = fotografaService;
    }

    /* =====================================================
       DASHBOARD DA FOTÓGRAFA (LOGADA)
       ===================================================== */

    @Operation(
            summary = "Retorna o perfil privado da fotógrafa logada",
            description = "Extrai a identidade pelo Token JWT (ID ou E-mail).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me")
    public Mono<ResponseEntity<Fotografa>> obterMeuPerfil(Principal principal) {
        String identificador = principal.getName();

        return fotografaService.findById(identificador)
                .switchIfEmpty(fotografaService.findByEmail(identificador))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /* =====================================================
       LEITURA PÚBLICA (LISTAGEM)
       ===================================================== */

    @Operation(summary = "Lista fotógrafas do acervo (visão pública protegida)")
    @GetMapping
    public Flux<FotografaPublicoDTO> listarPublico(
            @Parameter(description = "Filtrar por categoria: HISTORICA, ATIVA ou ESPOLIO")
            @RequestParam(required = false) Fotografa.CategoriaFotografa categoria
    ) {
        Flux<Fotografa> fotografas = fotografaService.findAllPublicas();

        if (categoria != null) {
            fotografas = fotografas.filter(f -> f.getCategoria() == categoria);
        }

        return fotografas.map(FotografaPublicoDTO::fromModel);
    }

    /* =====================================================
       LEITURA PÚBLICA (ID)
       ===================================================== */

    @Operation(summary = "Busca perfil público da fotógrafa pelo ID")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<FotografaPublicoDTO>> buscarPorId(@PathVariable String id) {
        return fotografaService.findPublicaById(id)
                .map(f -> ResponseEntity.ok(FotografaPublicoDTO.fromModel(f)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /* =====================================================
       PERFIL PÚBLICO COMPLETO (SLUG)
       ===================================================== */

    @Operation(summary = "Busca perfil público completo da fotógrafa pelo Slug (Dados + Coleção)")
    @GetMapping("/perfil/{slug}")
    public Mono<ResponseEntity<FotografaPerfilDTO>> buscarPerfilPorSlug(@PathVariable String slug) {
        return fotografaService.findPublicaBySlug(slug)
                .map(f -> ResponseEntity.ok(FotografaPerfilDTO.fromModel(f)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /* =====================================================
       CADASTRO (AUTOCADASTRO)
       ===================================================== */

    @Operation(summary = "Cadastro inicial de fotógrafa (pendente de aprovação)")
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Fotografa> cadastrar(@RequestBody Fotografa fotografa) {
        return fotografaService.createCadastro(fotografa);
    }

    /* =====================================================
       ADMIN – GESTÃO E VERIFICAÇÃO
       ===================================================== */

    @Operation(
            summary = "Atualiza dados da fotógrafa",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Fotografa>> atualizar(
            @PathVariable String id,
            @RequestBody Fotografa fotografa
    ) {
        return fotografaService.update(id, fotografa)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Aprova ou rejeita a fotógrafa",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{id}/verificacao")
    public Mono<ResponseEntity<Fotografa>> verificar(
            @PathVariable String id,
            @RequestParam Fotografa.StatusVerificacao status,
            @RequestParam(required = false) String observacoes
    ) {
        return fotografaService.verificarFotografa(id, status, observacoes)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Remove fotógrafa do acervo",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> remover(@PathVariable String id) {
        return fotografaService.deleteById(id)
                .then(Mono.just(new ResponseEntity<>(HttpStatus.NO_CONTENT)));
    }
}
