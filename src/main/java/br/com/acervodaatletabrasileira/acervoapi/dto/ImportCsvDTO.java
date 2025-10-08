// src/main/java/.../dto/ImportCsvDTO.java

package br.com.acervodaatletabrasileira.acervoapi.dto;

import org.springframework.web.multipart.MultipartFile;

// Record simples para receber o arquivo CSV
public record ImportCsvDTO(
        MultipartFile file
) {
    // Corpo vazio. O Spring fará a injeção do arquivo.
}