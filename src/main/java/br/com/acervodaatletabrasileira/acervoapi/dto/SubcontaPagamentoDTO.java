package br.com.acervodaatletabrasileira.acervoapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados complementares exigidos pelo Asaas para abertura de subconta
 * (KYC), que não fazem sentido manter permanentemente no model Atleta
 * por serem específicos do gateway de pagamento.
 *
 * Nome, e-mail e CPF são reaproveitados diretamente do cadastro da Atleta.
 */
@Schema(description = "Dados complementares para provisionar a conta de recebimento (Asaas) da atleta")
public record SubcontaPagamentoDTO(

        @Schema(example = "11999998888", description = "Celular com DDD, somente números")
        String celular,

        @Schema(example = "1990-05-12")
        LocalDate dataNascimento,

        @Schema(example = "01310930")
        String cep,

        @Schema(example = "Avenida Paulista")
        String endereco,

        @Schema(example = "1000")
        String numeroEndereco,

        @Schema(example = "Bela Vista")
        String bairro,

        @Schema(example = "5000.00", description = "Renda mensal declarada")
        BigDecimal rendaMensal
) {}
