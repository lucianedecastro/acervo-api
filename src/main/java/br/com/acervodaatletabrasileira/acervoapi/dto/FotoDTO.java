package br.com.acervodaatletabrasileira.acervoapi.dto;

// Este record representa os metadados de cada foto enviados pelo frontend.
// O 'id' pode ser o UUID de uma foto existente ou um ID temporário do frontend.
// O 'filename' é usado para associar este DTO ao arquivo de upload correspondente.
public record FotoDTO(
        String id,
        String legenda,
        boolean ehDestaque,
        String url,       // Presente apenas para fotos já existentes
        String filename   // Usado para mapear com os novos arquivos
) {
}