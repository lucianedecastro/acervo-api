package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Transacao;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repositório para gestão financeira das licenças de uso.
 * Permite o rastreio de receitas da plataforma e repasses para as atletas.
 */
@Repository
public interface TransacaoRepository extends ReactiveMongoRepository<Transacao, String> {

    /**
     * Busca todas as transações de uma atleta específica.
     * Útil para gerar o extrato de repasse (justiça econômica).
     */
    Flux<Transacao> findByAtletaId(String atletaId);

    /**
     * Busca transações por status financeiro (ex: PENDENTE, REPASSADO).
     */
    Flux<Transacao> findByStatusFinanceiro(String statusFinanceiro);

    /**
     * Busca todas as licenças adquiridas por um comprador/pesquisador específico.
     */
    Flux<Transacao> findByCompradorId(String compradorId);
}