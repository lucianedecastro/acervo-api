package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.AdminDashboardStatsDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaDashboardStatsDTO;
import br.com.acervodaatletabrasileira.acervoapi.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/dashboard")
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
    public Mono<AtletaDashboardStatsDTO> getAtletaDashboard(Authentication authentication) {
        /* O name do Principal geralmente contém o ID ou Email do usuário logado,
           dependendo de como você configurou o seu UserDetails no CustomUserDetailsService.
        */
        String atletaId = authentication.getName();
        return service.getAtletaStats(atletaId);
    }
}