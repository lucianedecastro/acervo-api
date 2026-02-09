package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.ModalidadeDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Modalidade;
import br.com.acervodaatletabrasileira.acervoapi.repository.ModalidadeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ModalidadeService {

    private static final Logger log = LoggerFactory.getLogger(ModalidadeService.class);

    private final ModalidadeRepository repository;
    private final CloudinaryService cloudinaryService;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public ModalidadeService(
            ModalidadeRepository repository,
            CloudinaryService cloudinaryService
    ) {
        this.repository = repository;
        this.cloudinaryService = cloudinaryService;
    }

    /* ==========================
       LEITURA
       ========================== */

    public Flux<Modalidade> findAll() {
        return repository.findAll();
    }

    public Mono<Modalidade> findById(String id) {
        return repository.findById(id);
    }

    public Mono<Modalidade> findBySlug(String slug) {
        return repository.findBySlug(slug);
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

        modalidade.setFotos(dto.fotos());
        modalidade.setFotoDestaquePublicId(dto.fotoDestaquePublicId());

        modalidade.setSlug(generateSlug(dto.nome()));
        modalidade.setCriadoEm(Instant.now());
        modalidade.setAtualizadoEm(Instant.now());

        return validateFotoDestaqueIfPresent(dto.fotoDestaquePublicId())
                .then(repository.save(modalidade));
    }

    /* ==========================
       ATUALIZAÇÃO (ADMIN)
       ========================= */

    public Mono<Modalidade> update(String id, ModalidadeDTO dto) {
        return repository.findById(id)
                .switchIfEmpty(
                        Mono.error(new IllegalArgumentException("Modalidade não encontrada com o ID: " + id))
                )
                .flatMap(existing ->
                        validateFotoDestaqueIfPresent(dto.fotoDestaquePublicId())
                                .then(Mono.fromCallable(() -> {

                                    if (dto.nome() != null && !existing.getNome().equalsIgnoreCase(dto.nome())) {
                                        existing.setSlug(generateSlug(dto.nome()));
                                        existing.setNome(dto.nome());
                                    }

                                    if (dto.historia() != null) existing.setHistoria(dto.historia());
                                    if (dto.pictogramaUrl() != null) existing.setPictogramaUrl(dto.pictogramaUrl());
                                    if (dto.ativa() != null) existing.setAtiva(dto.ativa());

                                    if (dto.fotos() != null) existing.setFotos(dto.fotos());
                                    if (dto.fotoDestaquePublicId() != null) {
                                        existing.setFotoDestaquePublicId(dto.fotoDestaquePublicId());
                                    }

                                    existing.setAtualizadoEm(Instant.now());
                                    return existing;
                                }))
                )
                .flatMap(repository::save);
    }

    /* ==========================
       DELETE (ADMIN)
       ========================== */

    public Mono<Void> deleteById(String id) {
        return repository.deleteById(id);
    }

    /* ==========================
       UPLOAD (ADMIN)
       ========================== */

    public Mono<Modalidade> uploadFotoDestaque(String modalidadeId, FilePart file) {
        return repository.findById(modalidadeId)
                .switchIfEmpty(
                        Mono.error(new IllegalArgumentException("Modalidade não encontrada com o ID: " + modalidadeId))
                )
                .flatMap(modalidade ->
                        cloudinaryService.uploadImagem(
                                        file,
                                        "modalidades/" + modalidade.getSlug()
                                )
                                .flatMap(result -> {

                                    String publicId = (String) result.get("publicId");

                                    modalidade.setFotoDestaquePublicId(publicId);
                                    modalidade.setAtualizadoEm(Instant.now());

                                    log.info(
                                            "Upload de foto de destaque realizado para modalidade '{}' com publicId '{}'",
                                            modalidade.getNome(),
                                            publicId
                                    );

                                    return repository.save(modalidade);
                                })
                );
    }

    /* ==========================
       UTIL (Validação Cloudinary)
       ========================== */

    private Mono<Void> validateFotoDestaqueIfPresent(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return Mono.empty();
        }

        return cloudinaryService.resourceExists(publicId)
                .flatMap(exists -> {
                    if (!exists) {
                        log.warn(
                                "Foto de destaque ainda não existe no Cloudinary para o publicId: {}. " +
                                        "Referência será salva e o upload será tratado posteriormente.",
                                publicId
                        );
                    }
                    return Mono.empty();
                });
    }

    /* ==========================
       UTIL (Gerador de Slugs)
       ========================== */

    private String generateSlug(String input) {
        if (input == null) return null;

        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");

        return slug.toLowerCase(Locale.ENGLISH)
                .replaceAll("-{2,}", "-")
                .replaceAll("(^-|-$)", "");
    }
}
