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
    private AtletaRepository atletaRepository;      // ⛽ GASOLINA

    @Autowired
    private FirestoreDirectService directService;   // 🌽 ÁLCOOL

    // 🌽 ÁLCOOL - SALVAR (O PROBLEMÁTICO)
    public Mono<Atleta> save(Atleta atleta) {
        return directService.saveAtleta(atleta);
    }

    // ⛽ GASOLINA - BUSCAR TODOS
    public Flux<Atleta> findAll() {
        return atletaRepository.findAll();
    }

    // ⛽ GASOLINA - BUSCAR POR ID
    public Mono<Atleta> findById(String id) {
        return atletaRepository.findById(id);
    }

    // 🌽/⛽ HÍBRIDO - ATUALIZAR
    public Mono<Atleta> update(String id, Atleta atleta) {
        return atletaRepository.findById(id)
                .flatMap(existingAtleta -> {
                    // Atualiza apenas se o campo novo não for nulo
                    if (atleta.getNome() != null) {
                        existingAtleta.setNome(atleta.getNome());
                    }
                    if (atleta.getBiografia() != null) {
                        existingAtleta.setBiografia(atleta.getBiografia());
                    }
                    if (atleta.getModalidade() != null) {
                        existingAtleta.setModalidade(atleta.getModalidade());
                    }
                    if (atleta.getCompeticao() != null) {
                        existingAtleta.setCompeticao(atleta.getCompeticao());
                    }

                    // Atualiza galeria se vier algo novo
                    if (atleta.getFotos() != null && !atleta.getFotos().isEmpty()) {
                        existingAtleta.setFotos(atleta.getFotos());
                    }

                    // 🌽 USA ÁLCOOL PRA SALVAR A ATUALIZAÇÃO
                    return directService.saveAtleta(existingAtleta);
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrado para atualização: " + id)));
    }

    // ⛽ GASOLINA - DELETAR
    public Mono<Void> deleteById(String id) {
        return atletaRepository.deleteById(id);
    }
}