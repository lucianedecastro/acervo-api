package br.com.acervodaatletabrasileira.acervoapi.service;

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

    // --- MÉTODOS DE LEITURA ---
    public Flux<Modalidade> findAll() {
        return repository.findAll();
    }

    public Mono<Modalidade> findById(String id) {
        return repository.findById(id);
    }

    // --- MÉTODOS DE ESCRITA (usando a estratégia híbrida) ---

    // Salvar uma nova modalidade (usa o Álcool 🌽)
    public Mono<Modalidade> save(Modalidade modalidade) {
        // A lógica de upload do pictograma será feita no Controller
        return directService.saveModalidade(modalidade);
    }

    // Atualizar uma modalidade existente
    public Mono<Modalidade> update(String id, Modalidade modalidade) {
        return repository.findById(id)
                .flatMap(existingModalidade -> {
                    existingModalidade.setNome(modalidade.getNome());
                    existingModalidade.setHistoria(modalidade.getHistoria());
                    // A URL do pictograma será atualizada se uma nova for fornecida
                    if (modalidade.getPictogramaUrl() != null) {
                        existingModalidade.setPictogramaUrl(modalidade.getPictogramaUrl());
                    }
                    // Salva a atualização usando o Álcool 🌽
                    return directService.saveModalidade(existingModalidade);
                });
    }

    // Deletar uma modalidade
    public Mono<Void> deleteById(String id) {
        return repository.findById(id)
                .flatMap(modalidade -> {
                    Mono<Void> deletePictogramMono = Mono.empty();
                    // Se houver um pictograma, cria uma tarefa para deletá-lo
                    if (modalidade.getPictogramaUrl() != null && !modalidade.getPictogramaUrl().isBlank()) {
                        deletePictogramMono = storageService.deleteFile(modalidade.getPictogramaUrl());
                    }
                    // Executa a deleção do pictograma e SÓ DEPOIS deleta a modalidade do banco
                    return deletePictogramMono.then(repository.deleteById(id));
                })
                .then();
    }
}