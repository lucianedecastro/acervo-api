package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoItemAcervo;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para criação/edição de itens.
 * Inclui o marcador de item histórico para diferenciar pesquisa de marketplace.
 */
public record ItemAcervoCreateDTO(
        String titulo,
        String descricao,
        String local,
        String dataOriginal,

        /**
         * Procedência: Identifica a atleta dona do acervo original.
         */
        String procedencia,
        String fotografoDoador,

        TipoItemAcervo tipo,
        StatusItemAcervo status,

        /**
         * Inteligência de Licenciamento (Pilar de Sustentabilidade)
         */
        BigDecimal precoBaseLicenciamento,
        Boolean disponivelParaLicenciamento,

        // Novo: Define se o item é apenas para o memorial/pesquisa
        Boolean itemHistorico,

        String restricoesUso,

        String modalidadeId,
        List<String> atletasIds,
        List<FotoDTO> fotos,
        String curadorResponsavel
) {
}