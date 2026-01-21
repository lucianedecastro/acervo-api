package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.Fotografa;

import java.util.List;

public record FotografaPerfilDTO(
        String id,
        String nome,
        String nomeSocial,
        String slug,

        // Identidade profissional
        String registroProfissional,
        String linkRedeSocialPrincipal,

        // Conteúdo editorial
        String biografia,

        // Destaques
        List<String> fotosDestaque,
        String fotoDestaqueUrl,

        // Navegação do acervo
        String linkColecaoCompleta,

        // Status público
        Fotografa.CategoriaFotografa categoria,
        Fotografa.StatusVerificacao statusVerificacao,
        Fotografa.StatusFotografa statusFotografa
) {

    /**
     * Conversão segura da Model para Perfil Público da Fotógrafa
     * (não expõe dados financeiros, jurídicos ou de autenticação)
     */
    public static FotografaPerfilDTO fromModel(Fotografa fotografa) {
        return new FotografaPerfilDTO(
                fotografa.getId(),
                fotografa.getNome(),
                fotografa.getNomeSocial(),
                fotografa.getSlug(),
                fotografa.getRegistroProfissional(),
                fotografa.getLinkRedeSocialPrincipal(),
                fotografa.getBiografia(),
                fotografa.getFotosDestaque(),
                fotografa.getFotoDestaqueUrl(),
                fotografa.getLinkColecaoCompleta(),
                fotografa.getCategoria(),
                fotografa.getStatusVerificacao(),
                fotografa.getStatusFotografa()
        );
    }
}

