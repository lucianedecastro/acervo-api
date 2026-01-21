package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoItemAcervo;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para criação e edição de itens do acervo.
 * Usado por Admin e Atleta (e futuramente Fotógrafa).
 */
public record ItemAcervoCreateDTO(

        // Identidade editorial
        String titulo,
        String descricao,
        String local,
        String dataOriginal,

        /**
         * Procedência do item (ex: "Acervo pessoal de X", "Domínio público")
         */
        String procedencia,

        /**
         * Crédito autoral exibível
         * (mantido por compatibilidade; pode evoluir depois)
         */
        String fotografoDoador,

        // Tipificação
        TipoItemAcervo tipo,
        StatusItemAcervo status,

        /**
         * Licenciamento
         */
        BigDecimal precoBaseLicenciamento,
        Boolean disponivelParaLicenciamento,

        /**
         * Define se o item é apenas histórico (memorial/pesquisa)
         */
        Boolean itemHistorico,

        /**
         * Restrições específicas de uso
         */
        String restricoesUso,

        // Relacionamentos
        String modalidadeId,
        List<String> atletasIds,

        /**
         * Fotos associadas (metadados)
         */
        List<FotoDTO> fotos,

        /**
         * Curador responsável pela validação
         */
        String curadorResponsavel
) {
}
