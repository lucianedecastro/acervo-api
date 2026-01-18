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

    public ItemAcervoService(ItemAcervoRepository repository, CloudinaryService cloudinaryService) {
        this.repository = repository;
        this.cloudinaryService = cloudinaryService;
    }

    /* =====================================================
       CONSULTAS PÚBLICAS (FILTRADAS POR STATUS)
       ===================================================== */

    public Flux<ItemAcervoResponseDTO> listarPublicados() {
        return repository.findByStatus(StatusItemAcervo.PUBLICADO)
                .concatWith(repository.findByStatus(StatusItemAcervo.DISPONIVEL_LICENCIAMENTO))
                .map(this::toResponseDTO);
    }

    public Mono<ItemAcervoResponseDTO> buscarPublicadoPorId(String id) {
        return repository.findById(id)
                .filter(item ->
                        StatusItemAcervo.PUBLICADO.equals(item.getStatus()) ||
                                StatusItemAcervo.DISPONIVEL_LICENCIAMENTO.equals(item.getStatus())
                )
                .map(this::toResponseDTO);
    }

    public Flux<ItemAcervoResponseDTO> listarPublicadosPorAtleta(String atletaId) {
        return repository.findByAtletasIdsContainingAndStatus(atletaId, StatusItemAcervo.PUBLICADO)
                .map(this::toResponseDTO);
    }

    public Flux<ItemAcervoResponseDTO> listarPublicadosPorModalidade(String modalidadeId) {
        return repository.findByModalidadeIdAndStatus(modalidadeId, StatusItemAcervo.PUBLICADO)
                .map(this::toResponseDTO);
    }

    /* =====================================================
       CURADORIA / ADMIN (FOCO EM ACERVO PESSOAL)
       ===================================================== */

    public Flux<ItemAcervo> listarTodos() {
        return repository.findAll();
    }

    public Mono<ItemAcervo> criar(ItemAcervoCreateDTO dto) {
        ItemAcervo item = new ItemAcervo();

        item.setTitulo(dto.titulo());
        item.setDescricao(dto.descricao());
        item.setLocal(dto.local());
        item.setDataOriginal(dto.dataOriginal());

        // Atribui a procedência do acervo pessoal [cite: 7, 15]
        item.setProcedencia(dto.procedencia());
        item.setFotografoDoador(dto.fotografoDoador());

        item.setTipo(dto.tipo());
        item.setStatus(dto.status() != null ? dto.status() : StatusItemAcervo.RASCUNHO);
        item.setModalidadeId(dto.modalidadeId());
        item.setAtletasIds(dto.atletasIds());

        // Define regras para circulação remunerada [cite: 16, 17]
        item.setPrecoBaseLicenciamento(dto.precoBaseLicenciamento());
        item.setDisponivelParaLicenciamento(dto.disponivelParaLicenciamento());
        item.setRestricoesUso(dto.restricoesUso());
        item.setCuradorResponsavel(dto.curadorResponsavel());

        item.setFotos(mapFotos(dto.fotos()));
        item.setCriadoEm(Instant.now());
        item.setAtualizadoEm(Instant.now());

        return repository.save(item);
    }

    public Mono<ItemAcervo> atualizar(String id, ItemAcervoCreateDTO dto) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                .flatMap(existente -> {
                    existente.setTitulo(dto.titulo());
                    existente.setDescricao(dto.descricao());
                    existente.setLocal(dto.local());
                    existente.setDataOriginal(dto.dataOriginal());
                    existente.setProcedencia(dto.procedencia());
                    existente.setFotografoDoador(dto.fotografoDoador());
                    existente.setTipo(dto.tipo());
                    existente.setStatus(dto.status());
                    existente.setPrecoBaseLicenciamento(dto.precoBaseLicenciamento());
                    existente.setDisponivelParaLicenciamento(dto.disponivelParaLicenciamento());
                    existente.setRestricoesUso(dto.restricoesUso());
                    existente.setModalidadeId(dto.modalidadeId());
                    existente.setAtletasIds(dto.atletasIds());
                    existente.setFotos(mapFotos(dto.fotos()));
                    existente.setAtualizadoEm(Instant.now());

                    return repository.save(existente);
                });
    }

    public Mono<ItemAcervo> publicar(String id) {
        return repository.findById(id)
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
       UPLOAD DE MÍDIA (CLOUDINARY)
       ===================================================== */

    public Mono<FotoDTO> uploadCloudinaryPuro(FilePart file) {
        return cloudinaryService.uploadImagem(file, "acervo")
                .map(result -> new FotoDTO(
                        result.get("public_id").toString(),
                        "Upload Avulso",
                        false,
                        result.get("url").toString(),
                        null
                ));
    }

    public Mono<FotoDTO> adicionarFoto(String itemId, FilePart file, FotoDTO metadata) {
        return repository.findById(itemId)
                .flatMap(item -> cloudinaryService.uploadImagem(file, "acervo")
                        .flatMap(result -> {
                            FotoAcervo foto = new FotoAcervo();
                            foto.setPublicId(result.get("public_id").toString());
                            foto.setUrlVisualizacao(result.get("url").toString());
                            foto.setLegenda(metadata.legenda());
                            foto.setDestaque(Boolean.TRUE.equals(metadata.ehDestaque()));

                            if (item.getFotos() == null) item.setFotos(new ArrayList<>());
                            item.getFotos().add(foto);
                            item.setAtualizadoEm(Instant.now());

                            return repository.save(item).thenReturn(toFotoDTO(foto));
                        }));
    }

    /* =====================================================
       MAPPERS
       ===================================================== */

    private ItemAcervoResponseDTO toResponseDTO(ItemAcervo item) {
        return new ItemAcervoResponseDTO(
                item.getId(),
                item.getTitulo(),
                item.getDescricao(),
                item.getLocal(),
                item.getDataOriginal(),
                item.getProcedencia(),
                item.getTipo(),
                item.getStatus(),
                item.getPrecoBaseLicenciamento(),
                item.getDisponivelParaLicenciamento(),
                item.getModalidadeId(),
                item.getAtletasIds(),
                item.getFotos() == null ? List.of() : item.getFotos().stream().map(this::toFotoDTO).toList(),
                item.getCriadoEm(),
                item.getAtualizadoEm()
        );
    }

    private FotoDTO toFotoDTO(FotoAcervo foto) {
        return new FotoDTO(
                foto.getPublicId(),
                foto.getLegenda(),
                foto.isDestaque(),
                foto.getUrlVisualizacao(),
                null
        );
    }

    private List<FotoAcervo> mapFotos(List<FotoDTO> fotos) {
        if (fotos == null) return List.of();
        return fotos.stream()
                .map(dto -> {
                    FotoAcervo foto = new FotoAcervo();
                    foto.setPublicId(dto.id() != null ? dto.id() : UUID.randomUUID().toString());
                    foto.setUrlVisualizacao(dto.url());
                    foto.setLegenda(dto.legenda());
                    foto.setDestaque(Boolean.TRUE.equals(dto.ehDestaque()));
                    return foto;
                }).toList();
    }
}