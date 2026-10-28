package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.FotografaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.UsuarioAdminRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * O login procura o e-mail em admins, atletas e fotógrafas (nessa ordem),
 * então o mesmo e-mail não pode existir em duas coleções.
 */
@Service
public class ContaEmailService {

    private final UsuarioAdminRepository admins;
    private final AtletaRepository atletas;
    private final FotografaRepository fotografas;

    public ContaEmailService(UsuarioAdminRepository admins, AtletaRepository atletas, FotografaRepository fotografas) {
        this.admins = admins;
        this.atletas = atletas;
        this.fotografas = fotografas;
    }

    public Mono<Void> garantirEmailLivre(String email) {
        return Mono.zip(
                        admins.findByEmail(email).hasElement(),
                        atletas.findByEmail(email).hasElement(),
                        fotografas.findByEmail(email).hasElement())
                .<Void>flatMap(existe -> existe.getT1() || existe.getT2() || existe.getT3()
                        ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado"))
                        : Mono.empty());
    }
}
