package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ModalidadeDTO; // ✅ Importar o DTO
import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import br.com.acervodaatletabrasileira.acervoapi.repository.ModalidadeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ModalidadeService {

    @Autowired
    private ModalidadeRepository repository;

    @Autowired
    private FirestoreDirectService directService;

    @Autowired
    private CloudStorageService storageService;

    // --- MÉTODOS DE LEITURA (Sem alterações) ---
    public Flux<Modalidade> findAll() {
        return repository.findAll();
    }

    public Mono<Modalidade> findById(String id) {
        return repository.findById(id);
    }

    // --- MÉTODOS DE ESCRITA (Sem alterações no save) ---
    public Mono<Modalidade> save(Modalidade modalidade) {
        return directService.saveModalidade(modalidade);
    }

    // ✅ MÉTODO UPDATE CORRIGIDO E OTIMIZADO
    // Agora ele recebe o DTO e a nova URL do pictograma separadamente.
    public Mono<Modalidade> update(String id, ModalidadeDTO dto, String pictogramaUrl) {
        return repository.findById(id)
                .flatMap(existingModalidade -> {
                    // Atualiza os campos da entidade existente com os dados do DTO
                    existingModalidade.setNome(dto.nome());
                    existingModalidade.setHistoria(dto.historia());

                    // A URL do pictograma só é atualizada se uma nova for fornecida.
                    // Se pictogramaUrl for null, significa que nenhum novo arquivo foi enviado.
                    if (pictogramaUrl != null) {
                        existingModalidade.setPictogramaUrl(pictogramaUrl);
                    }

                    // Salva a entidade já existente e atualizada
                    return directService.saveModalidade(existingModalidade);
                });
    }

    // --- MÉTODO DE DELETE (Sem alterações) ---
    public Mono<Void> deleteById(String id) {
        return repository.findById(id)
                .flatMap(modalidade -> {
                    Mono<Void> deletePictogramMono = Mono.empty();
                    if (modalidade.getPictogramaUrl() != null && !modalidade.getPictogramaUrl().isBlank()) {
                        deletePictogramMono = storageService.deleteFile(modalidade.getPictogramaUrl());
                    }
                    return deletePictogramMono.then(repository.deleteById(id));
                })
                .then();
    }
}