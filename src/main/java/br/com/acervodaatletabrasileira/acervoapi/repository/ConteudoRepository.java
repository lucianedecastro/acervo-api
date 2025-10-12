package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Conteudo;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConteudoRepository extends FirestoreReactiveRepository<Conteudo> {
    // ✅ CORREÇÃO: Nenhum método customizado é necessário.
    // Como o 'slug' está anotado com @DocumentId, o método findById(String id)
    // do FirestoreReactiveRepository agora vai buscar pelo slug automaticamente.
}