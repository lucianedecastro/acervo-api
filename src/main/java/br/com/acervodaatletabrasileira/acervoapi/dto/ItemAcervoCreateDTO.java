package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoItemAcervo;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para criação/edição de itens com foco em acervos pessoais e licenciamento.
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
        String restricoesUso,

        String modalidadeId,
        List<String> atletasIds,
        List<FotoDTO> fotos,
        String curadorResponsavel
) {
}