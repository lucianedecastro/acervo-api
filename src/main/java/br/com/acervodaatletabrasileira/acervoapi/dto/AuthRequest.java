// src/main/java/br/com/acervodaatletabrasileira/acervoapi/dto/AuthRequest.java
package br.com.acervodaatletabrasileira.acervoapi.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String senha;
}
