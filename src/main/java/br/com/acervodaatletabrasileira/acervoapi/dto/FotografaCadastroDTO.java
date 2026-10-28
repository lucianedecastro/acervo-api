package br.com.acervodaatletabrasileira.acervoapi.dto;

import br.com.acervodaatletabrasileira.acervoapi.model.Fotografa;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Entrada do autocadastro público de fotógrafa.
 *
 * Só contém campos que a própria fotógrafa pode informar. Campos de governança
 * (status, contrato, percentuais, conta no gateway, role) não existem aqui de
 * propósito: com a entidade como @RequestBody, qualquer pessoa poderia enviá-los.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FotografaCadastroDTO(
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 150) String nomeSocial,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha,
        @Size(max = 14) String cpf,
        @Size(max = 5000) String biografia,
        @Size(max = 100) String registroProfissional,
        @Size(max = 500) String linkRedeSocialPrincipal,
        @Size(max = 500) String linkColecaoCompleta,
        Fotografa.CategoriaFotografa categoria
) {

    public Fotografa toModel() {
        Fotografa f = new Fotografa();
        f.setNome(nome.trim());
        f.setNomeSocial(nomeSocial);
        f.setEmail(email.trim().toLowerCase());
        f.setSenha(senha);
        f.setCpf(cpf);
        f.setBiografia(biografia);
        f.setRegistroProfissional(registroProfissional);
        f.setLinkRedeSocialPrincipal(linkRedeSocialPrincipal);
        f.setLinkColecaoCompleta(linkColecaoCompleta);
        if (categoria != null) {
            f.setCategoria(categoria);
        }
        return f;
    }
}
