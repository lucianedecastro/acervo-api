package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AdminDashboardStatsDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaDashboardStatsDTO;
import br.com.acervodaatletabrasileira.acervoapi.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequestMapping("/dashboard") // Removido o /api para manter o padrão das outras rotas
@Tag(name = "Dashboard", description = "Visão gerencial e individual do acervo")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @Operation(
            summary = "Resumo estatístico Admin (Visão Global)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<AdminDashboardStatsDTO> getAdminDashboard() {
        return service.getAdminStats();
    }

    @Operation(
            summary = "Resumo estatístico da Atleta (Visão Individual)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/atleta")
    @PreAuthorize("hasRole('ATLETA')")
    public Mono<AtletaDashboardStatsDTO> getAtletaDashboard(Principal principal) {
        // O getName() contém o identificador (e-mail ou ID) vindo do JWT
        String identificador = principal != null ? principal.getName() : null;

        if (identificador == null || identificador.isBlank()) {
            return Mono.error(new IllegalStateException("Identificador da atleta inválido no token"));
        }

        return service.getAtletaStats(identificador.trim());
    }
}
