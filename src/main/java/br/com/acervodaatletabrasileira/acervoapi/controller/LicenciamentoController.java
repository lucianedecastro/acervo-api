package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ExtratoAtletaDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.PropostaLicenciamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.SimulacaoFaturamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.TransacaoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.service.LicenciamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/licenciamento")
@Tag(name = "Financeiro & Licenciamento", description = "Endpoints para simulação, efetivação de vendas e extratos")
public class LicenciamentoController {

    private final LicenciamentoService service;

    public LicenciamentoController(LicenciamentoService service) {
        this.service = service;
    }

    @Operation(summary = "Gera uma simulação de faturamento (Cálculo de repasse)")
    @PostMapping("/simular")
    public Mono<SimulacaoFaturamentoDTO> gerarProposta(@RequestBody PropostaLicenciamentoDTO proposta) {
        return service.gerarSimulacaoFaturamento(proposta);
    }

    @Operation(
            summary = "Efetiva a venda de uma licença e gera a transação",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/efetivar")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TransacaoResponseDTO> concluirLicenciamento(@RequestBody PropostaLicenciamentoDTO proposta) {
        return service.efetivarLicenciamento(proposta);
    }

    @Operation(
            summary = "Consulta histórico de transações de uma atleta",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/extrato/atleta/{atletaId}")
    public Flux<TransacaoResponseDTO> consultarExtratoAtleta(@PathVariable String atletaId) {
        return service.listarTransacoesPorAtleta(atletaId);
    }

    @Operation(
            summary = "Consulta extrato consolidado (Saldo + Histórico) da atleta",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/extrato/consolidado/{atletaId}")
    public Mono<ExtratoAtletaDTO> obterExtratoConsolidado(@PathVariable String atletaId) {
        return service.gerarExtratoConsolidado(atletaId);
    }
}