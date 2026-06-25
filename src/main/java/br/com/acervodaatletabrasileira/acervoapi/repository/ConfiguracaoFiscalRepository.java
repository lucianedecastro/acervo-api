package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.ConfiguracaoFiscal;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório para gestão das regras fiscais e percentuais de repasse.
 * Opera sobre o documento único de configuração global do sistema.
 */
@Repository
public interface ConfiguracaoFiscalRepository extends ReactiveMongoRepository<ConfiguracaoFiscal, String> {
}