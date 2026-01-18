package br.com.acervodaatletabrasileira.acervoapi.dto;

public record AuthRequest(
        String email,
        String senha
) {
}
