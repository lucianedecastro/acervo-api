package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoItemAcervo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO de resposta pública.
 * Exibe se o item é um registro histórico ou um ativo disponível para licenciamento.
 */
public record ItemAcervoResponseDTO(
        String id,
        String titulo,
        String descricao,
        String local,
        String dataOriginal,
        String procedencia,

        TipoItemAcervo tipo,
        StatusItemAcervo status,

        /**
         * Informações de Licenciamento
         */
        BigDecimal precoBaseLicenciamento,
        Boolean disponivelParaLicenciamento,

        // Novo: Para o frontend decidir o layout (Pesquisa vs. Compra)
        Boolean itemHistorico,

        String modalidadeId,
        List<String> atletasIds,
        List<FotoDTO> fotos,

        Instant criadoEm,
        Instant atualizadoEm
) {
}