package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.Collection;

@Repository
public interface ItemAcervoRepository extends ReactiveMongoRepository<ItemAcervo, String> {

    /* ==========================
       CONSULTAS PÚBLICAS E LICENCIAMENTO
       ========================== */

    // Permite buscar itens que estejam PUBLICADOS ou DISPONIVEL_LICENCIAMENTO
    Flux<ItemAcervo> findByStatusIn(Collection<StatusItemAcervo> statuses);

    // Feed geral por status único (mantido para compatibilidade)
    Flux<ItemAcervo> findByStatus(StatusItemAcervo status);

    // Itens por atleta e status (essencial para o pilar de memória das atletas)
    Flux<ItemAcervo> findByAtletasIdsContainingAndStatus(String atletaId, StatusItemAcervo status);

    // Itens por modalidade e status
    Flux<ItemAcervo> findByModalidadeIdAndStatus(String modalidadeId, StatusItemAcervo status);

    /* ==========================
       CONSULTAS ADMIN / CURADORIA (GESTÃO DE ACERVOS PESSOAIS)
       ========================= */

    Flux<ItemAcervo> findByAtletasIdsContaining(String atletaId);

    Flux<ItemAcervo> findByModalidadeId(String modalidadeId);

    // Busca por procedência (para encontrar itens de um acervo pessoal específico)
    Flux<ItemAcervo> findByProcedenciaContainingIgnoreCase(String termo);
}