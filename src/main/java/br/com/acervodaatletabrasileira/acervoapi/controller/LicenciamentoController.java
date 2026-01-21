package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ExtratoAtletaDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.PropostaLicenciamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.SimulacaoFaturamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.TransacaoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Licenciamento;
import br.com.acervodaatletabrasileira.acervoapi.service.LicenciamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/licenciamento")
@Tag(
        name = "Licenciamento & Financeiro",
        description = "Simulação, licenciamento jurídico e efeitos financeiros"
)
public class LicenciamentoController {

    private final LicenciamentoService service;

    public LicenciamentoController(LicenciamentoService service) {
        this.service = service;
    }

    /* =====================================================
       SIMULAÇÃO (PÚBLICA / PROTEGIDA POR CONTEXTO)
       ===================================================== */

    @Operation(summary = "Gera uma simulação de faturamento (sem efetivar licenciamento)")
    @PostMapping("/simular")
    public Mono<SimulacaoFaturamentoDTO> gerarSimulacao(
            @RequestBody PropostaLicenciamentoDTO proposta
    ) {
        return service.gerarSimulacaoFaturamento(proposta);
    }

    /* =====================================================
       LICENCIAMENTO (ATO FORMAL)
       ===================================================== */

    @Operation(
            summary = "Efetiva um licenciamento autorizado (gera transação)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/efetivar")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA')")
    public Mono<TransacaoResponseDTO> efetivarLicenciamento(
            @RequestBody PropostaLicenciamentoDTO proposta
    ) {
        return service.efetivarLicenciamento(proposta);
    }

    /* =====================================================
       CONSULTAS ADMIN / GOVERNANÇA
       ===================================================== */

    @Operation(
            summary = "Lista todos os licenciamentos (governança)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<Licenciamento> listarTodosLicenciamentos() {
        return service.listarTodosLicenciamentos();
    }

    @Operation(
            summary = "Lista licenciamentos por item de acervo",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin/item/{itemAcervoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<Licenciamento> listarPorItem(
            @PathVariable String itemAcervoId
    ) {
        return service.listarLicenciamentosPorItem(itemAcervoId);
    }

    /* =====================================================
       EXTRATOS FINANCEIROS
       ===================================================== */

    @Operation(
            summary = "Consulta histórico financeiro da atleta",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/extrato/atleta/{atletaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA')")
    public Flux<TransacaoResponseDTO> consultarExtratoAtleta(
            @PathVariable String atletaId
    ) {
        return service.listarTransacoesPorAtleta(atletaId);
    }

    @Operation(
            summary = "Consulta extrato consolidado da atleta",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/extrato/consolidado/{atletaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATLETA')")
    public Mono<ExtratoAtletaDTO> obterExtratoConsolidado(
            @PathVariable String atletaId
    ) {
        return service.gerarExtratoConsolidado(atletaId);
    }
}
