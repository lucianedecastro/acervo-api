package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ConfiguracaoFiscalDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.ConfiguracaoFiscal;
import br.com.acervodaatletabrasileira.acervoapi.repository.ConfiguracaoFiscalRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Instant;

@RestController
@RequestMapping("/configuracoes/fiscal")
@Tag(name = "Configurações Fiscais", description = "Gestão de taxas e percentuais (Exclusivo Admin)")
public class ConfiguracaoFiscalController {

    private final ConfiguracaoFiscalRepository repository;
    private static final String CONFIG_ID = "GLOBAL_SETTINGS";

    public ConfiguracaoFiscalController(ConfiguracaoFiscalRepository repository) {
        this.repository = repository;
    }

    @Operation(summary = "Consulta as taxas atuais", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ConfiguracaoFiscal> getAtual() {
        return repository.findById(CONFIG_ID);
    }

    @Operation(summary = "Atualiza as taxas financeiras", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ConfiguracaoFiscal> atualizar(@RequestBody ConfiguracaoFiscalDTO dto, Principal principal) {
        return repository.findById(CONFIG_ID)
                .defaultIfEmpty(new ConfiguracaoFiscal())
                .flatMap(config -> {
                    config.setId(CONFIG_ID);
                    config.setPercentualRepasseAtleta(dto.percentualRepasseAtleta());
                    config.setPercentualComissaoPlataforma(dto.percentualComissaoPlataforma());
                    config.setObservacaoLegal(dto.observacaoLegal());

                    // Auditoria preenchida pelo Sistema, não pelo DTO
                    config.setAtualizadoEm(Instant.now());
                    config.setAtualizadoPor(principal.getName());

                    return repository.save(config);
                });
    }
}