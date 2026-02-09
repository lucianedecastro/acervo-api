package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotoPerfilPublicaDTO;
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
        name = "Atletas - Foto de Destaque",
        description = "Upload e gerenciamento da foto de destaque (hero) do perfil público da atleta"
)
public class AtletaFotoDestaqueController {

    private final AtletaService atletaService;
    private final CloudinaryService cloudinaryService;

    public AtletaFotoDestaqueController(
            AtletaService atletaService,
            CloudinaryService cloudinaryService
    ) {
        this.atletaService = atletaService;
        this.cloudinaryService = cloudinaryService;
    }

    @Operation(
            summary = "Faz upload da foto de destaque (hero) da atleta",
            description = "Realiza o upload da imagem no Cloudinary e associa como foto de destaque pública. " +
                    "Esta rota é EXCLUSIVA para a imagem hero do perfil público.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(
            value = "/{id}/foto-destaque",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.OK)
    public Mono<ResponseEntity<FotoPerfilPublicaDTO>> uploadFotoDestaque(
            @PathVariable String id,
            @RequestPart("file") FilePart file
    ) {

        return atletaService.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada")))
                .flatMap(atleta ->
                        cloudinaryService.uploadImagem(file, "destaque/" + id)
                                .flatMap(uploadResult ->
                                        atualizarAtletaComFotoDestaque(atleta.getId(), uploadResult, file)
                                )
                )
                .map(fotoDestaque ->
                        ResponseEntity.ok(
                                new FotoPerfilPublicaDTO(
                                        fotoDestaque.getPublicId(),
                                        fotoDestaque.getUrl()
                                )
                        )
                );
    }

    /**
     * Atualiza a atleta com a nova foto de destaque (hero),
     * utilizando método específico do service,
     * sem acoplamento com update editorial.
     */
    private Mono<FotoPerfilAtleta> atualizarAtletaComFotoDestaque(
            String atletaId,
            Map<String, Object> uploadResult,
            FilePart file
    ) {

        String publicId = (String) uploadResult.get("publicId");
        String url = (String) uploadResult.get("url");

        FotoPerfilAtleta fotoDestaque = new FotoPerfilAtleta(
                publicId,
                url,
                file.filename(),
                Instant.now(),
                true
        );

        return atletaService
                .atualizarFotoDestaque(atletaId, fotoDestaque)
                .thenReturn(fotoDestaque);
    }
}
