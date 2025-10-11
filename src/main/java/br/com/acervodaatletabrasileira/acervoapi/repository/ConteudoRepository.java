package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Conteudo;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConteudoRepository extends FirestoreReactiveRepository<Conteudo> {
    // A interface herda todos os métodos de CRUD reativos necessários:
    // save, findById, findAll, deleteById, etc.

}
