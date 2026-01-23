package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import java.util.List;

public record AtletaPublicoDTO(
        String id,
        String nome,
        String nomeSocial,
        String slug,
        List<String> modalidadesIds,
        String biografia,
        Atleta.CategoriaAtleta categoria,
        Atleta.StatusVerificacao statusVerificacao,

        // LEGADO — NÃO MEXER
        String fotoDestaqueUrl,

        // NOVO — foto de perfil pública (opcional)
        FotoPerfilPublicaDTO fotoPerfil,

        String statusAtleta
) {

    // Método auxiliar para converter a Model para este DTO "limpo"
    public static AtletaPublicoDTO fromModel(Atleta atleta) {
        return new AtletaPublicoDTO(
                atleta.getId(),
                atleta.getNome(),
                atleta.getNomeSocial(),
                atleta.getSlug(),
                atleta.getModalidadesIds(),
                atleta.getBiografia(),
                atleta.getCategoria(),
                atleta.getStatusVerificacao(),

                // campo legado preservado
                atleta.getFotoDestaqueUrl(),

                // novo campo (null-safe, não quebra frontend)
                atleta.getFotoPerfil() != null
                        ? new FotoPerfilPublicaDTO(
                        atleta.getFotoPerfil().getPublicId(),
                        atleta.getFotoPerfil().getUrl()
                )
                        : null,

                atleta.getStatusAtleta()
        );
    }
}
