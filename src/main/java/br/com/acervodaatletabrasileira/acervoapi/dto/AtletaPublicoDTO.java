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

        /**
         * FOTO DE DESTAQUE (HERO)
         * - Novo fluxo tem prioridade
         * - Campo legado é fallback
         */
        String fotoDestaqueUrl,

        /**
         * FOTO DE PERFIL (AVATAR)
         * - Nunca é hero
         * - Usada apenas como fallback visual
         */
        FotoPerfilPublicaDTO fotoPerfil,

        String statusAtleta
) {

    public static AtletaPublicoDTO fromModel(Atleta atleta) {

        /* =========================
           FOTO DE PERFIL (AVATAR)
           ========================= */
        FotoPerfilPublicaDTO fotoPerfil = atleta.getFotoPerfil() != null
                ? new FotoPerfilPublicaDTO(
                atleta.getFotoPerfil().getPublicId(),
                atleta.getFotoPerfil().getUrl()
        )
                : null;

        /* =========================
           FOTO DE DESTAQUE (HERO)
           REGRA DEFINITIVA:
           1. Novo fluxo
           2. Legado
           ========================= */
        String fotoDestaqueUrl = atleta.getFotoDestaque() != null
                ? atleta.getFotoDestaque().getUrl()
                : atleta.getFotoDestaqueUrl();

        return new AtletaPublicoDTO(
                atleta.getId(),
                atleta.getNome(),
                atleta.getNomeSocial(),
                atleta.getSlug(),
                atleta.getModalidadesIds(),
                atleta.getBiografia(),
                atleta.getCategoria(),
                atleta.getStatusVerificacao(),
                fotoDestaqueUrl,
                fotoPerfil,
                atleta.getStatusAtleta()
        );
    }
}
