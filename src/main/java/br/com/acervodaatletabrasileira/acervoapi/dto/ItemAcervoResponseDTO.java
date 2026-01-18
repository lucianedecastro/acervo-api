package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoItemAcervo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO de resposta pública.
 * Focado na visualização histórica e no estímulo ao licenciamento.
 */
public record ItemAcervoResponseDTO(
        String id,
        String titulo,
        String descricao,
        String local,
        String dataOriginal,
        String procedencia, // Ex: "Acervo Pessoal de Luciane Castro"

        TipoItemAcervo tipo,
        StatusItemAcervo status,

        /**
         * Informações para o interessado em licenciar o conteúdo
         */
        BigDecimal precoBaseLicenciamento,
        Boolean disponivelParaLicenciamento,

        String modalidadeId,
        List<String> atletasIds,
        List<FotoDTO> fotos, // Aqui o Service deve garantir que sejam as URLs de visualização

        Instant criadoEm,
        Instant atualizadoEm
) {
}