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

    // --- MÉTODO UPDATE REFORÇADO ---
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

                    return atletaRepository.save(existingAtleta);
                })
                // Caso o atleta não exista, lança erro específico
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrado para atualização: " + id)));
    }
    // --- FIM DO MÉTODO UPDATE REFORÇADO ---

    public Mono<Void> deleteById(String id) {
        // Futuramente, aqui também entrará a lógica de auditoria
        return atletaRepository.deleteById(id);
    }
}
