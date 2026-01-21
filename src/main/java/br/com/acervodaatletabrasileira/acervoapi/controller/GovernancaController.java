package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.model.LogDecisao;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoDecisao;
import br.com.acervodaatletabrasileira.acervoapi.service.GovernancaExportService;
import br.com.acervodaatletabrasileira.acervoapi.service.GovernancaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/admin/governanca")
@Tag(
        name = "Governança / Auditoria",
        description = "Auditoria, rastreabilidade e histórico de decisões do sistema"
)
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class GovernancaController {

    private final GovernancaService governancaService;
    private final GovernancaExportService exportService;

    public GovernancaController(
            GovernancaService governancaService,
            GovernancaExportService exportService
    ) {
        this.governancaService = governancaService;
        this.exportService = exportService;
    }

    /* =====================================================
       CONSULTA FILTRADA
       ===================================================== */

    @Operation(summary = "Lista logs de governança por período")
    @GetMapping("/logs")
    public Mono<List<LogDecisao>> listarPorPeriodo(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant inicio,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant fim
    ) {
        return governancaService.listarPorPeriodo(inicio, fim)
                .collectList();
    }

    /* =====================================================
       EXPORTAÇÃO CSV
       ===================================================== */

    @Operation(summary = "Exporta logs de governança em CSV por período")
    @GetMapping("/logs/export/csv")
    public Mono<ResponseEntity<byte[]>> exportarCsv(
            @RequestParam(required = false) Instant inicio,
            @RequestParam(required = false) Instant fim
    ) {
        return governancaService.listarPorPeriodo(inicio, fim)
                .collectList()
                .map(logs -> ResponseEntity.ok()
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"logs_governanca.csv\""
                        )
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .body(exportService.exportarCsv(logs))
                );
    }

    /* =====================================================
       EXPORTAÇÃO PDF
       ===================================================== */

    @Operation(summary = "Exporta relatório de governança em PDF")
    @GetMapping("/logs/export/pdf")
    public Mono<ResponseEntity<byte[]>> exportarPdf(
            @RequestParam(required = false) Instant inicio,
            @RequestParam(required = false) Instant fim
    ) {
        return governancaService.listarPorPeriodo(inicio, fim)
                .collectList()
                .map(logs -> {

                    String conteudo =
                            exportService.gerarConteudoPdf(logs, inicio, fim);

                    return ResponseEntity.ok()
                            .header(
                                    HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"relatorio_governanca.pdf\""
                            )
                            .contentType(MediaType.APPLICATION_PDF)
                            .body(conteudo.getBytes());
                });
    }
}
