// src/main/java/br/com/acervodaatletabrasileira/acervoapi/service/AtletaService.java

package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AtletaService {

    @Autowired
    private AtletaRepository atletaRepository;

    public Flux<Atleta> findAll() {
        return atletaRepository.findAll();
    }

    public Mono<Atleta> findById(String id) {
        return atletaRepository.findById(id);
    }

    public Mono<Atleta> save(Atleta atleta) {
        // Futuramente, aqui entrará a lógica de auditoria (RF08, RN03)
        return atletaRepository.save(atleta);
    }

    public Mono<Atleta> update(String id, Atleta atleta) {
        return atletaRepository.findById(id)
                .flatMap(existingAtleta -> {
                    existingAtleta.setNome(atleta.getNome());
                    existingAtleta.setBiografia(atleta.getBiografia());
                    existingAtleta.setModalidade(atleta.getModalidade());
                    existingAtleta.setCompeticao(atleta.getCompeticao());
                    existingAtleta.setImagemUrl(atleta.getImagemUrl());
                    return atletaRepository.save(existingAtleta);
                });
    }

    public Mono<Void> deleteById(String id) {
        // Futuramente, aqui também entrará a lógica de auditoria
        return atletaRepository.deleteById(id);
    }
}
