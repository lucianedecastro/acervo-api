package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
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

    /**
     * Salva atleta diretamente no Firestore, contornando o Spring Data bugado
     */
    public Mono<Atleta> saveAtleta(Atleta atleta) {
        return Mono.fromCallable(() -> {
            // 🎯 GERA ID MANUALMENTE - SEM MAGIA DO SPRING!
            if (atleta.getId() == null) {
                atleta.setId(UUID.randomUUID().toString());
                System.out.println("🔑 ID gerado manualmente: " + atleta.getId());
            }

            // 🎯 SALVA DIRETO NO FIRESTORE - CONTROLE TOTAL!
            DocumentReference docRef = firestore.collection("atletas").document(atleta.getId());
            docRef.set(atleta).get();

            System.out.println("💾 Atleta salva diretamente no Firestore: " + atleta.getId());
            return atleta;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}