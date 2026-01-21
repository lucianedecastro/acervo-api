package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoItemAcervo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO de resposta pública do acervo.
 * Usado para pesquisa histórica e vitrine de licenciamento.
 */
public record ItemAcervoResponseDTO(

        String id,

        // Conteúdo editorial
        String titulo,
        String descricao,
        String local,
        String dataOriginal,

        /**
         * Procedência do item (texto livre)
         */
        String procedencia,

        // Tipificação e status
        TipoItemAcervo tipo,
        StatusItemAcervo status,

        /**
         * Informações de licenciamento (quando aplicável)
         */
        BigDecimal precoBaseLicenciamento,
        Boolean disponivelParaLicenciamento,

        /**
         * Indica se o item pertence ao memorial histórico
         */
        Boolean itemHistorico,

        // Relacionamentos
        String modalidadeId,
        List<String> atletasIds,

        /**
         * Fotos visíveis (preview / marca d’água)
         */
        List<FotoDTO> fotos,

        // Auditoria
        Instant criadoEm,
        Instant atualizadoEm
) {
}
