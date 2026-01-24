package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta.TipoChavePix;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta.CategoriaAtleta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para captura de dados no cadastro e atualização de Atletas.
 *
 * ⚠️ ATENÇÃO (IMPORTANTE PARA DEVS FUTUROS):
 * Este DTO é utilizado em DOIS contextos distintos:
 *
 * 1. Cadastro inicial da atleta
 * 2. Atualização administrativa via Dashboard
 *
 * Campos sensíveis como CPF, EMAIL e SENHA:
 * - DEVEM ser utilizados apenas no cadastro inicial
 * - NÃO devem ser sobrescritos em fluxos de atualização administrativa
 *
 * A proteção contra sobrescrita desses campos deve ser feita no Service.
 */
@Schema(description = "Formulário de cadastro/atualização de Atleta (Frentes Histórica e Financeira)")
public record AtletaFormDTO(

        // ==========================
        // IDENTIDADE BÁSICA
        // ==========================

        @Schema(example = "Maria Lenk")
        String nome,

        @Schema(example = "Maria Lenk")
        String nomeSocial,

        /**
         * CPF da atleta.
         * ⚠️ Campo sensível: uso exclusivo no cadastro inicial.
         * Não deve ser alterado por fluxos administrativos.
         */
        @Schema(example = "12345678901")
        String cpf,

        /**
         * E-mail da atleta (identidade de autenticação).
         * ⚠️ Campo sensível e crítico:
         * - Utilizado como username no login
         * - NÃO deve ser alterado em updates administrativos
         */
        @Schema(example = "contato@marialenk.org.br")
        String email,

        /**
         * Senha de acesso ao dashboard da atleta.
         * ⚠️ Deve ser utilizada apenas:
         * - no cadastro inicial
         * - ou em fluxos explícitos de reset de senha
         */
        @Schema(description = "Senha para acesso ao dashboard da atleta", example = "senha123")
        String senha,

        // ==========================
        // VÍNCULOS ESPORTIVOS
        // ==========================

        @Schema(description = "Lista de IDs das modalidades vinculadas")
        List<String> modalidades,

        @Schema(example = "Pioneira da natação brasileira...")
        String biografia,

        // ==========================
        // CATEGORIA E REPRESENTAÇÃO
        // ==========================

        @Schema(description = "Tipo de perfil (HISTORICA, ATIVA ou ESPOLIO)")
        CategoriaAtleta categoria,

        String nomeRepresentante,
        String cpfRepresentante,
        String vinculoRepresentante,

        // ==========================
        // GOVERNANÇA E CONTRATOS
        // ==========================

        Boolean contratoAssinado,
        String linkContratoDigital,
        String dadosContato,

        // ==========================
        // DADOS FINANCEIROS
        // ==========================

        TipoChavePix tipoChavePix,
        String chavePix,
        String banco,
        String agencia,
        String conta,
        String tipoConta,

        BigDecimal comissaoPlataformaDiferenciada,

        // ==========================
        // MÍDIA E STATUS
        // ==========================

        /**
         * ID ou URL da foto principal.
         * Campo legado mantido para compatibilidade.
         */
        @Schema(description = "ID ou URL da foto principal")
        String fotoDestaqueId,

        @Schema(example = "ATIVO")
        String statusAtleta
) {}
