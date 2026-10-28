package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta.CategoriaAtleta;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada do autocadastro público de atleta ("Criar conta").
 * Só traz o que a própria atleta pode informar; status, contrato e dados
 * financeiros são definidos pela plataforma/curadoria.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AtletaRegistroDTO(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha,
        @NotBlank @Size(max = 14) String cpf,
        CategoriaAtleta categoria
) {}
