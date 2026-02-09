package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotoPerfilPublicaDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoPerfilAtleta;
import br.com.acervodaatletabrasileira.acervoapi.service.AtletaService;
import br.com.acervodaatletabrasileira.acervoapi.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/atletas")
@Tag(
        name = "Atletas - Foto de Perfil",
        description = "Upload e gerenciamento da foto de perfil pública da atleta"
)
public class AtletaFotoPerfilController {

    private final AtletaService atletaService;
    private final CloudinaryService cloudinaryService;

    public AtletaFotoPerfilController(
            AtletaService atletaService,
            CloudinaryService cloudinaryService
    ) {
        this.atletaService = atletaService;
        this.cloudinaryService = cloudinaryService;
    }

    @Operation(
            summary = "Faz upload da foto de perfil pública da atleta",
            description = "Realiza o upload da imagem no Cloudinary e atualiza a foto de perfil da atleta. " +
                    "Esta rota é a ÚNICA forma oficial de enviar foto de perfil.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(
            value = "/{id}/foto-perfil",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public Mono<ResponseEntity<FotoPerfilPublicaDTO>> uploadFotoPerfil(
            @PathVariable String id,
            @RequestPart("file") FilePart file
    ) {

        return atletaService.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada")))
                .flatMap(atleta ->
                        cloudinaryService.uploadImagem(file, "perfil/" + id)
                                .flatMap(uploadResult ->
                                        atualizarAtletaComFotoPerfil(atleta.getId(), uploadResult, file)
                                )
                )
                .map(fotoPerfil ->
                        ResponseEntity.ok(
                                new FotoPerfilPublicaDTO(
                                        fotoPerfil.getPublicId(),
                                        fotoPerfil.getUrl()
                                )
                        )
                );
    }

    /**
     * Atualiza a atleta com a nova foto de perfil,
     * utilizando o método específico do service,
     * sem acoplar com o fluxo geral de update.
     */
    private Mono<FotoPerfilAtleta> atualizarAtletaComFotoPerfil(
            String atletaId,
            Map<String, Object> uploadResult,
            FilePart file
    ) {

        String publicId = (String) uploadResult.get("publicId");
        String url = (String) uploadResult.get("url");

        FotoPerfilAtleta fotoPerfil = new FotoPerfilAtleta(
                publicId,
                url,
                file.filename(),
                Instant.now(),
                true
        );

        return atletaService
                .atualizarFotoPerfil(atletaId, fotoPerfil)
                .thenReturn(fotoPerfil);
    }
}
