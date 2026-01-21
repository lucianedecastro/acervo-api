package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Licenciamento;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface LicenciamentoRepository
        extends ReactiveMongoRepository<Licenciamento, String> {

    Flux<Licenciamento> findByItemAcervoId(String itemAcervoId);

    Flux<Licenciamento> findByAtletaId(String atletaId);
}


