package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ExtratoAtletaDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.PropostaLicenciamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.SimulacaoFaturamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.TransacaoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Licenciamento;
import br.com.acervodaatletabrasileira.acervoapi.service.LicenciamentoService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Escondido do Swagger: módulo de licenciamento/pagamento pausado
 * até a frente de monetização entrar em produção. Rotas continuam
 * ativas e protegidas (ADMIN/ATLETA), só não aparecem na documentação
 * pública pra não dar a impressão de feature pronta pra uso.
 */
@Hidden
@RestController
@RequestMapping("/licenciamento")
@Tag(
        name = "Licenciamento & Financeiro",
        description = """
                Infraestrutura de simulação, formalização de licenciamento
                e controle financeiro institucional.
                
                Diretriz atual:
                - Blockchain NÃO é registrada automaticamente.
                - O carimbo institucional ocorre via Governança/Admin.
                - Este módulo apenas consolida o ato jurídico-financeiro.
                """
)
public class LicenciamentoController {

    private final LicenciamentoService service;

    public LicenciamentoController(LicenciamentoService service) {
        this.service = service;
    }

    /* =====================================================
       SIMULAÇÃO (PÚBLICA / CONTEXTUAL)
       ===================================================== */

    @Operation(
            summary = "Gera simulação de faturamento",
            description = "Calcula o split entre plataforma e atleta/espólio conforme regras vigentes."
    )
    @PostMapping("/simular")
    public Mono<SimulacaoFaturamentoDTO> gerarSimulacao(
            @RequestBody PropostaLicenciamentoDTO proposta
    ) {
        return service.gerarSimulacaoFaturamento(proposta);
    }

    /* =====================================================
       LICENCIAMENTO (ATO FORMAL JURÍDICO-FINANCEIRO)
       ===================================================== */

    @Operation(
            summary = "Efetiva licenciamento autorizado",
            description = """
                    Consolida o licenciamento no sistema:
                    - Validação jurídica
                    - Registro financeiro
                    - Geração de transação interna
                    
                    O registro na Blockchain ocorre posteriormente
                    via fluxo administrativo de Governança.
                    """,
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

    @Operation(
            summary = "Registra o selo institucional (Blockchain) de um licenciamento já liquidado",
            description = """
                    Ato administrativo separado da efetivação.
                    Só pode ser executado após a confirmação de pagamento (Transação LIQUIDADA).
                    Aprova o licenciamento e grava o TxId na Blockchain, espelhado também
                    na Transação vinculada.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/admin/{licenciamentoId}/registrar-blockchain")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Licenciamento> registrarSeloInstitucional(
            @PathVariable String licenciamentoId,
            Authentication authentication
    ) {
        return service.registrarSeloInstitucional(licenciamentoId, authentication.getName());
    }

    /* =====================================================
       CONSULTAS ADMINISTRATIVAS
       ===================================================== */

    @Operation(
            summary = "Lista todos os licenciamentos",
            description = "Uso exclusivo administrativo para auditoria interna.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<Licenciamento> listarTodosLicenciamentos() {
        return service.listarTodosLicenciamentos();
    }

    @Operation(
            summary = "Lista licenciamentos por item",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin/item/{itemAcervoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<Licenciamento> listarPorItem(
            @PathVariable String itemAcervoId
    ) {
        return service.listarLicenciamentosPorItem(itemAcervoId);
    }

    @Operation(
            summary = "Lista todas as transações (visão administrativa geral)",
            description = "Lista completa, sem filtro por atleta — o front aplica os filtros (status, período).",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin/transacoes")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<TransacaoResponseDTO> listarTodasTransacoes() {
        return service.listarTodasTransacoes();
    }

    /* =====================================================
       EXTRATOS FINANCEIROS
       ===================================================== */

    @Operation(
            summary = "Consulta histórico financeiro da atleta",
            description = """
                    Lista detalhada de repasses.
                    O status de Blockchain pode estar:
                    - Aguardando registro institucional
                    - Registrado
                    - Em auditoria
                    """,
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