package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ModalidadeDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import br.com.acervodaatletabrasileira.acervoapi.repository.ModalidadeRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ModalidadeService {

    private final ModalidadeRepository repository;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public ModalidadeService(ModalidadeRepository repository) {
        this.repository = repository;
    }

    /* ==========================
       LEITURA (PÚBLICA)
       ========================== */

    public Flux<Modalidade> findAll() {
        // Busca apenas as ativas diretamente no MongoDB
        return repository.findByAtivaTrue();
    }

    public Mono<Modalidade> findById(String id) {
        return repository.findById(id)
                .filter(m -> Boolean.TRUE.equals(m.getAtiva()));
    }

    public Mono<Modalidade> findBySlug(String slug) {
        return repository.findBySlug(slug)
                .filter(m -> Boolean.TRUE.equals(m.getAtiva()));
    }

    /* ==========================
       CRIAÇÃO (ADMIN)
       ========================== */

    public Mono<Modalidade> create(ModalidadeDTO dto) {
        Modalidade modalidade = new Modalidade();
        modalidade.setNome(dto.nome());
        modalidade.setHistoria(dto.historia());
        modalidade.setPictogramaUrl(dto.pictogramaUrl());
        modalidade.setAtiva(dto.ativa() != null ? dto.ativa() : true);

        modalidade.setSlug(generateSlug(dto.nome()));
        modalidade.setCriadoEm(Instant.now());
        modalidade.setAtualizadoEm(Instant.now());

        return repository.save(modalidade);
    }

    /* ==========================
       ATUALIZAÇÃO (ADMIN)
       ========================= */

    public Mono<Modalidade> update(String id, ModalidadeDTO dto) {
        return repository.findById(id)
                .switchIfEmpty(
                        Mono.error(new IllegalArgumentException("Modalidade não encontrada"))
                )
                .flatMap(existing -> {
                    // Se o nome mudou, atualizamos o slug
                    if (!existing.getNome().equalsIgnoreCase(dto.nome())) {
                        existing.setSlug(generateSlug(dto.nome()));
                    }

                    existing.setNome(dto.nome());
                    existing.setHistoria(dto.historia());
                    existing.setPictogramaUrl(dto.pictogramaUrl());
                    existing.setAtiva(dto.ativa() != null ? dto.ativa() : existing.getAtiva());
                    existing.setAtualizadoEm(Instant.now());

                    return repository.save(existing);
                });
    }

    /* ==========================
       DELETE (ADMIN)
       ========================== */

    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    /* ==========================
       UTIL
       ========================== */

    private String generateSlug(String input) {
        if (input == null) return null;

        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");

        return slug.toLowerCase(Locale.ENGLISH)
                .replaceAll("-{2,}", "-") // Remove hifens duplos
                .replaceAll("(^-|-$)", ""); // Remove hifens no início ou fim
    }
}