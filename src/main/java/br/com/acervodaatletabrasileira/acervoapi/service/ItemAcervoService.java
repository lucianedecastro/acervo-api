package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoCreateDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ItemAcervoService {

    private final ItemAcervoRepository repository;
    private final CloudinaryService cloudinaryService;

    public ItemAcervoService(
            ItemAcervoRepository repository,
            CloudinaryService cloudinaryService
    ) {
        this.repository = repository;
        this.cloudinaryService = cloudinaryService;
    }

    /* =====================================================
       CONSULTAS PÚBLICAS (APENAS PUBLICADOS)
       ===================================================== */

    public Flux<ItemAcervoResponseDTO> listarPublicados() {
        return repository
                .findByStatus(StatusItemAcervo.PUBLICADO)
                .map(this::toResponseDTO);
    }

    public Flux<ItemAcervoResponseDTO> listarPublicadosPorAtleta(String atletaId) {
        return repository
                .findByAtletasIdsContainingAndStatus(
                        atletaId,
                        StatusItemAcervo.PUBLICADO
                )
                .map(this::toResponseDTO);
    }

    public Flux<ItemAcervoResponseDTO> listarPublicadosPorModalidade(String modalidadeId) {
        return repository
                .findByModalidadeIdAndStatus(
                        modalidadeId,
                        StatusItemAcervo.PUBLICADO
                )
                .map(this::toResponseDTO);
    }

    public Mono<ItemAcervoResponseDTO> buscarPublicadoPorId(String id) {
        return repository.findById(id)
                .filter(item -> item.getStatus() == StatusItemAcervo.PUBLICADO)
                .map(this::toResponseDTO);
    }

    /* =====================================================
       CURADORIA / ADMIN
       ===================================================== */

    public Flux<ItemAcervo> listarTodos() {
        return repository.findAll();
    }

    public Mono<ItemAcervo> criar(ItemAcervoCreateDTO dto) {

        ItemAcervo item = new ItemAcervo();
        item.setTitulo(dto.titulo());
        item.setDescricao(dto.descricao());
        item.setTipo(dto.tipo());
        item.setStatus(
                dto.status() != null ? dto.status() : StatusItemAcervo.RASCUNHO
        );
        item.setModalidadeId(dto.modalidadeId());
        item.setAtletasIds(dto.atletasIds());
        item.setFotos(mapFotos(dto.fotos()));
        item.setCriadoEm(Instant.now());
        item.setAtualizadoEm(Instant.now());

        return repository.save(item);
    }

    public Mono<ItemAcervo> atualizar(String id, ItemAcervoCreateDTO dto) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Item do acervo não encontrado")
                ))
                .flatMap(existente -> {

                    existente.setTitulo(dto.titulo());
                    existente.setDescricao(dto.descricao());
                    existente.setTipo(dto.tipo());
                    existente.setStatus(dto.status());
                    existente.setModalidadeId(dto.modalidadeId());
                    existente.setAtletasIds(dto.atletasIds());
                    existente.setFotos(mapFotos(dto.fotos()));
                    existente.setAtualizadoEm(Instant.now());

                    return repository.save(existente);
                });
    }

    public Mono<ItemAcervo> publicar(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Item do acervo não encontrado")
                ))
                .flatMap(item -> {
                    item.setStatus(StatusItemAcervo.PUBLICADO);
                    item.setAtualizadoEm(Instant.now());
                    return repository.save(item);
                });
    }

    public Mono<Void> remover(String id) {
        return repository.deleteById(id);
    }

    /* =====================================================
       UPLOAD DE FOTO (ADMIN)
       ===================================================== */

    /**
     * Realiza o upload apenas no Cloudinary, sem salvar no banco de dados.
     * Útil para o formulário de Atletas no Frontend.
     */
    public Mono<FotoDTO> uploadCloudinaryPuro(FilePart file) {
        return cloudinaryService.uploadImagem(file, "atletas")
                .map(result -> new FotoDTO(
                        UUID.randomUUID().toString(),
                        "Upload Avulso",
                        false,
                        result.get("url"),
                        null
                ));
    }

    public Mono<FotoDTO> adicionarFoto(
            String itemId,
            FilePart file,
            FotoDTO metadata
    ) {

        return repository.findById(itemId)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Item do acervo não encontrado")
                ))
                .flatMap(item ->
                        cloudinaryService
                                .uploadImagem(file, "acervo")
                                .flatMap(result -> {

                                    FotoAcervo foto = new FotoAcervo(
                                            result.get("url"),
                                            metadata.legenda()
                                    );

                                    foto.setId(UUID.randomUUID().toString());
                                    foto.setEhDestaque(metadata.ehDestaque());

                                    List<FotoAcervo> fotos = item.getFotos();
                                    if (fotos == null) {
                                        fotos = new ArrayList<>();
                                    }

                                    if (Boolean.TRUE.equals(foto.getEhDestaque())) {
                                        fotos.forEach(f -> f.setEhDestaque(false));
                                    }

                                    fotos.add(foto);
                                    item.setFotos(fotos);
                                    item.setAtualizadoEm(Instant.now());

                                    return repository.save(item)
                                            .thenReturn(toFotoDTO(foto));
                                })
                );
    }

    /* =====================================================
       MAPPERS
       ===================================================== */

    private ItemAcervoResponseDTO toResponseDTO(ItemAcervo item) {
        return new ItemAcervoResponseDTO(
                item.getId(),
                item.getTitulo(),
                item.getDescricao(),
                item.getTipo(),
                item.getStatus(),
                item.getModalidadeId(),
                item.getAtletasIds(),
                item.getFotos() == null
                        ? List.of()
                        : item.getFotos().stream().map(this::toFotoDTO).toList(),
                item.getCriadoEm(),
                item.getAtualizadoEm()
        );
    }

    private FotoDTO toFotoDTO(FotoAcervo foto) {
        return new FotoDTO(
                foto.getId(),
                foto.getLegenda(),
                Boolean.TRUE.equals(foto.getEhDestaque()),
                foto.getUrl(),
                null
        );
    }

    private List<FotoAcervo> mapFotos(List<FotoDTO> fotos) {
        if (fotos == null || fotos.isEmpty()) {
            return List.of();
        }

        return fotos.stream()
                .map(dto -> {
                    FotoAcervo foto = new FotoAcervo(
                            dto.url(),
                            dto.legenda()
                    );

                    foto.setId(
                            dto.id() != null && !dto.id().isBlank()
                                    ? dto.id()
                                    : UUID.randomUUID().toString()
                    );

                    foto.setEhDestaque(dto.ehDestaque());
                    return foto;
                })
                .toList();
    }
}