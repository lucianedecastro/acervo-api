package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.Conteudo; // ✅ IMPORTAR
import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@Service
public class FirestoreDirectService {

    @Autowired
    private Firestore firestore;

    public Mono<Atleta> saveAtleta(Atleta atleta) {
        return Mono.fromCallable(() -> {
            if (atleta.getId() == null || atleta.getId().isBlank()) {
                atleta.setId(UUID.randomUUID().toString());
            }
            DocumentReference docRef = firestore.collection("atletas").document(atleta.getId());
            docRef.set(atleta).get();
            return atleta;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Modalidade> saveModalidade(Modalidade modalidade) {
        return Mono.fromCallable(() -> {
            if (modalidade.getId() == null || modalidade.getId().isBlank()) {
                modalidade.setId(UUID.randomUUID().toString());
            }
            DocumentReference docRef = firestore.collection("modalidades").document(modalidade.getId());
            docRef.set(modalidade).get();
            return modalidade;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // ✅ NOVO MÉTODO PARA SALVAR CONTEÚDOS
    /**
     * Salva um bloco de conteúdo diretamente no Firestore.
     */
    public Mono<Conteudo> saveConteudo(Conteudo conteudo) {
        return Mono.fromCallable(() -> {
            // O slug é o ID, então ele já deve existir.
            DocumentReference docRef = firestore.collection("conteudos").document(conteudo.getSlug());
            docRef.set(conteudo).get();
            return conteudo;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}