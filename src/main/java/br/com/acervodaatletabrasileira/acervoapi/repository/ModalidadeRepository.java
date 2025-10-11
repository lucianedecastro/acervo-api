package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModalidadeRepository extends FirestoreReactiveRepository<Modalidade> {
    // Por enquanto, os métodos padrão (save, findById, findAll, deleteById)
    // que vêm do FirestoreReactiveRepository são suficientes.
    // No futuro, se precisarmos de uma busca customizada, como "findByNome",
    // adicionaríamos a assinatura do método aqui.
}
