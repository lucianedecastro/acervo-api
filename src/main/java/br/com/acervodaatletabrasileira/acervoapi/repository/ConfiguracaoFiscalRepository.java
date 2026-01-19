package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.ConfiguracaoFiscal;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracaoFiscalRepository extends ReactiveMongoRepository<ConfiguracaoFiscal, String> {
}