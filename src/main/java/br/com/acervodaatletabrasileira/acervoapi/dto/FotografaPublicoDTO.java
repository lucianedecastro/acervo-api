package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.Fotografa;
import java.util.List;

public record FotografaPublicoDTO(
        String id,
        String nome,
        String nomeSocial,
        String slug,
        String registroProfissional,
        String linkRedeSocialPrincipal,
        String biografia,
        List<String> fotosDestaque,
        String linkColecaoCompleta,
        Fotografa.CategoriaFotografa categoria,
        Fotografa.StatusVerificacao statusVerificacao,
        String fotoDestaqueUrl,
        Fotografa.StatusFotografa statusFotografa
) {

    // Conversão segura da Model para DTO público
    public static FotografaPublicoDTO fromModel(Fotografa fotografa) {
        return new FotografaPublicoDTO(
                fotografa.getId(),
                fotografa.getNome(),
                fotografa.getNomeSocial(),
                fotografa.getSlug(),
                fotografa.getRegistroProfissional(),
                fotografa.getLinkRedeSocialPrincipal(),
                fotografa.getBiografia(),
                fotografa.getFotosDestaque(),
                fotografa.getLinkColecaoCompleta(),
                fotografa.getCategoria(),
                fotografa.getStatusVerificacao(),
                fotografa.getFotoDestaqueUrl(),
                fotografa.getStatusFotografa()
        );
    }
}
