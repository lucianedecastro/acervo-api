// src/main/java/br/com/acervodaatletabrasileira/acervoapi/repository/AtletaRepository.java

package br.com.acervodaatletabrasileira.acervoapi.repository;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AtletaRepository extends FirestoreReactiveRepository<Atleta> {
}

