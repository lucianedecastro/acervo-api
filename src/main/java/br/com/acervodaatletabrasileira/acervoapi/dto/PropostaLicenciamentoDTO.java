package br.com.acervodaatletabrasileira.acervoapi.dto;

/**
 * Proposta de licenciamento de item do acervo.
 *
 * Representa a intenção de uso, NÃO a autorização jurídica
 * nem a efetivação financeira.
 */
public record PropostaLicenciamentoDTO(

        /**
         * Item do acervo a ser licenciado
         */
        String itemAcervoId,

        /**
         * Atleta titular do acervo
         */
        String atletaId,

        /**
         * Fotógrafa autora do material (opcional).
         * Previsto para divisão futura de direitos e repasses.
         */
        String fotografaId,

        /**
         * Tipo de uso pretendido
         * Ex: COMERCIAL, EDITORIAL, INSTITUCIONAL
         */
        String tipoUso,

        /**
         * Prazo de uso em meses
         * (null = uso pontual / sem vigência definida)
         */
        Integer prazoMeses
) {}
