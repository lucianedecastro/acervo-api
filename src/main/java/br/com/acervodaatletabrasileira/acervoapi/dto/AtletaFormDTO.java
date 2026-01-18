package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta.TipoChavePix;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta.CategoriaAtleta;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para captura de dados no cadastro e atualização de Atletas.
 * Inclui campos de Categoria (Histórico/Ativa/Espólio) e Representação Legal.
 */
@Schema(description = "Formulário de cadastro/atualização de Atleta (Frentes Histórica e Financeira)")
public record AtletaFormDTO(
        @Schema(example = "Maria Lenk")
        String nome,

        @Schema(example = "Maria Lenk")
        String nomeSocial,

        @Schema(example = "12345678901")
        String cpf,

        @Schema(example = "contato@marialenk.org.br")
        String email,

        @Schema(description = "Senha para acesso ao dashboard da atleta", example = "senha123")
        String senha,

        @Schema(description = "Lista de IDs das modalidades vinculadas")
        List<String> modalidades,

        @Schema(example = "Pioneira da natação brasileira...")
        String biografia,

        // --- Categoria e Representação ---
        @Schema(description = "Tipo de perfil (HISTORICA, ATIVA ou ESPOLIO)")
        CategoriaAtleta categoria,

        String nomeRepresentante,
        String cpfRepresentante,
        String vinculoRepresentante,

        // --- Governança ---
        Boolean contratoAssinado,
        String linkContratoDigital,
        String dadosContato,

        // --- Dados Financeiros Estruturados ---
        TipoChavePix tipoChavePix,
        String chavePix,
        String banco,
        String agencia,
        String conta,
        String tipoConta,

        BigDecimal comissaoPlataformaDiferenciada,

        @Schema(description = "ID ou URL da foto principal")
        String fotoDestaqueId,

        @Schema(example = "ATIVO")
        String statusAtleta
) {}