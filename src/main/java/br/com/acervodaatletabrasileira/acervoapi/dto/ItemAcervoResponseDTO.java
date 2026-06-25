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

        /**
         * Crédito autoral exibível publicamente
         */
        String creditoAutoral,

        /* =====================================================
           PESQUISA E MEMÓRIA (INCREMENTO)
           ===================================================== */
        /**
         * Fonte primária da pesquisa (ex: "Hemeroteca Digital")
         */
        String fontePesquisa,

        /**
         * Link para a fonte original (se houver)
         */
        String linkFontePesquisa,

        /**
         * Indica se a obra está em domínio público (Isenta de royalties)
         */
        Boolean dominioPublico,

        /* =====================================================
           TRANSPARÊNCIA BLOCKCHAIN (INCREMENTO)
           ===================================================== */
        /**
         * Hash da Transação (TxId) na rede.
         * O "Selo de Autenticidade" que o frontend vai linkar para o explorador de blocos.
         */
        String blockchainTxId,

        /**
         * Data em que o registro se tornou imutável.
         */
        Instant dataRegistroBlockchain,

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