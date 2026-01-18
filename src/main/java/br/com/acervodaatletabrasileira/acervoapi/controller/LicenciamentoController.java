package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ExtratoAtletaDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.PropostaLicenciamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.SimulacaoFaturamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.TransacaoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.service.LicenciamentoService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/licenciamento")
public class LicenciamentoController {

    private final LicenciamentoService service;

    public LicenciamentoController(LicenciamentoService service) {
        this.service = service;
    }

    @PostMapping("/proposta")
    public Mono<SimulacaoFaturamentoDTO> gerarProposta(@RequestBody PropostaLicenciamentoDTO proposta) {
        return service.gerarSimulacaoFaturamento(proposta);
    }

    @PostMapping("/concluir")
    public Mono<TransacaoResponseDTO> concluirLicenciamento(@RequestBody PropostaLicenciamentoDTO proposta) {
        return service.efetivarLicenciamento(proposta);
    }

    /**
     * Endpoint para consulta de extrato financeiro detalhado da atleta.
     */
    @GetMapping("/extrato/atleta/{atletaId}")
    public Flux<TransacaoResponseDTO> consultarExtratoAtleta(@PathVariable String atletaId) {
        return service.listarTransacoesPorAtleta(atletaId);
    }

    /**
     * Novo Endpoint: Extrato Consolidado (Nome, Saldo Total e Histórico).
     * Ideal para visualização estilo "App Bancário" no navegador.
     */
    @GetMapping("/extrato/consolidado/{atletaId}")
    public Mono<ExtratoAtletaDTO> obterExtratoConsolidado(@PathVariable String atletaId) {
        return service.gerarExtratoConsolidado(atletaId);
    }
}