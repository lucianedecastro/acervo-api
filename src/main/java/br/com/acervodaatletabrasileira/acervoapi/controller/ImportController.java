package br.com.acervodaatletabrasileira.acervoapi.controller;

import br.com.acervodaatletabrasileira.acervoapi.dto.ImportCsvDTO;
import br.com.acervodaatletabrasileira.acervoapi.service.ImportService; // Novo serviço de importação
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin/import")
public class ImportController {

    private final ImportService importService;

    @Autowired
    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    @Operation(summary = "Importa atletas via arquivo CSV (Requer Autenticação)", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping(consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<String>> importAtletasFromCsv(@ModelAttribute ImportCsvDTO dto) {

        // 1. O Controller passa o arquivo CSV (MultipartFile) para o serviço
        return importService.processCsvUpload(dto.file())
                .then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).body("Importação de atletas iniciada com sucesso.")))
                // O onErrorResume é importante para capturar erros durante a leitura do CSV
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Falha na importação do CSV: " + e.getMessage())));
    }
}
