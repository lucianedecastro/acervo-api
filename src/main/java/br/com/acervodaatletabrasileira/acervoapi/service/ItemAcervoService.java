package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoCreateDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemAcervoService {

    private final ItemAcervoRepository repository;
    private final AtletaRepository atletaRepository;
    private final CloudinaryService cloudinaryService;

    private static final List<StatusItemAcervo> STATUS_PUBLICOS = List.of(
            StatusItemAcervo.PUBLICADO,
            StatusItemAcervo.DISPONIVEL_LICENCIAMENTO,
            StatusItemAcervo.MEMORIAL
    );

    public ItemAcervoService(
            ItemAcervoRepository repository,
            AtletaRepository atletaRepository,
            CloudinaryService cloudinaryService
    ) {
        this.repository = repository;
        this.atletaRepository = atletaRepository;
        this.cloudinaryService = cloudinaryService;
    }

    /* =====================================================
       CONSULTAS PÚBLICAS
       ===================================================== */

    public Flux<ItemAcervoResponseDTO> listarPublicados() {
        return repository.findByStatusIn(STATUS_PUBLICOS)
                .map(this::toResponseDTO);
    }

    public Mono<ItemAcervoResponseDTO> buscarPublicadoPorId(String id) {
        return repository.findById(id)
                .filter(item -> STATUS_PUBLICOS.contains(item.getStatus()))
                .map(this::toResponseDTO);
    }

    public Flux<ItemAcervoResponseDTO> listarPublicadosPorAtleta(String atletaId) {
        return repository
                .findByAtletasIdsContainingAndStatusIn(atletaId, STATUS_PUBLICOS)
                .map(this::toResponseDTO);
    }

    public Flux<ItemAcervoResponseDTO> listarPublicadosPorModalidade(String modalidadeId) {
        return repository
                .findByModalidadeIdAndStatus(modalidadeId, StatusItemAcervo.PUBLICADO)
                .map(this::toResponseDTO);
    }

    /* =====================================================
       CRIAÇÃO / ATUALIZAÇÃO
       ===================================================== */

    public Mono<ItemAcervo> criar(ItemAcervoCreateDTO dto) {
        ItemAcervo item = new ItemAcervo();

        if (dto.tipo() == null) {
            return Mono.error(new IllegalArgumentException("Tipo do item de acervo é obrigatório"));
        }

        if (dto.modalidadeId() == null || dto.modalidadeId().isBlank()) {
            return Mono.error(new IllegalArgumentException("Modalidade do item é obrigatória"));
        }

        if (dto.atletasIds() == null || dto.atletasIds().isEmpty()) {
            return Mono.error(
                    new IllegalArgumentException("Item de acervo deve estar vinculado a pelo menos uma atleta")
            );
        }

        preencherDadosComuns(item, dto);
        item.setCriadoEm(Instant.now());
        item.setAtualizadoEm(Instant.now());

        return repository.save(item);
    }

    public Mono<ItemAcervo> atualizarProtegido(
            String id,
            ItemAcervoCreateDTO dto,
            String identificadorUsuario,
            Set<String> roles
    ) {
        boolean isAtleta = roles.contains("ROLE_ATLETA");

        Mono<String> donoIdMono = isAtleta
                ? atletaRepository.findByEmail(identificadorUsuario)
                .map(a -> a.getId())
                .switchIfEmpty(Mono.error(new AccessDeniedException("Atleta não encontrada")))
                : Mono.just(identificadorUsuario);

        return donoIdMono.flatMap(idLogado ->
                repository.findById(id)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                        .flatMap(existente -> {

                            if (isAtleta && !existente.getAtletasIds().contains(idLogado)) {
                                return Mono.error(
                                        new AccessDeniedException("Sem permissão para editar este item")
                                );
                            }

                            preencherDadosComuns(existente, dto);
                            existente.setAtualizadoEm(Instant.now());
                            return repository.save(existente);
                        })
        );
    }

    /* =====================================================
       PUBLICAÇÃO / REMOÇÃO
       ===================================================== */

    public Mono<ItemAcervo> publicar(String id) {
        return repository.findById(id)
                .flatMap(item -> {
                    item.setStatus(Boolean.TRUE.equals(item.getItemHistorico())
                            ? StatusItemAcervo.MEMORIAL
                            : StatusItemAcervo.PUBLICADO);
                    item.setAtualizadoEm(Instant.now());
                    return repository.save(item);
                });
    }

    public Mono<Void> remover(String id) {
        return repository.deleteById(id);
    }

    public Flux<ItemAcervo> listarTodos() {
        return repository.findAll();
    }

    /* =====================================================
       UPLOAD E MÍDIA
       ===================================================== */

    public Mono<FotoDTO> uploadCloudinaryPuro(FilePart file) {
        return cloudinaryService.uploadImagem(file, "temp")
                .map(result -> FotoDTO.fromUpload(
                        (String) result.get("url"),
                        (String) result.get("publicId"),
                        (Long) result.get("version"),
                        "Upload avulso",
                        false
                ));
    }

    public Mono<FotoDTO> adicionarFoto(String itemId, FilePart file, FotoDTO metadata) {
        return repository.findById(itemId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                .flatMap(item ->
                        cloudinaryService.uploadImagem(file, "itens")
                                .flatMap(result -> {

                                    FotoAcervo foto = new FotoAcervo();

                                    foto.setPublicId((String) result.get("publicId"));

                                    // ✅ CORREÇÃO DO CAST (Integer → Long)
                                    Number versionNumber = (Number) result.get("version");
                                    foto.setVersion(versionNumber.longValue());

                                    // Arquiteturalmente, não persistimos URLs derivadas.
                                    // A URL de exibição é sempre construída no frontend a partir de publicId + version.
                                    foto.setUrlVisualizacao(null);

                                    foto.setLegenda(metadata.legenda());
                                    foto.setDestaque(Boolean.TRUE.equals(metadata.ehDestaque()));
                                    foto.setAutorNomePublico(metadata.autorNomePublico());
                                    foto.setLicenciamentoPermitido(
                                            Boolean.TRUE.equals(metadata.licenciamentoPermitido())
                                    );
                                    foto.setPossuiMarcaDagua(true);

                                    if (item.getFotos() == null) {
                                        item.setFotos(new ArrayList<>());
                                    }

                                    item.getFotos().add(foto);
                                    item.setAtualizadoEm(Instant.now());

                                    return repository.save(item)
                                            .thenReturn(toFotoDTO(foto));
                                })
                );
    }

    /* =====================================================
       MAPEAMENTOS
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
                item.getItemHistorico(),
                item.getModalidadeId(),
                item.getAtletasIds(),
                item.getFotos() == null
                        ? List.of()
                        : item.getFotos().stream()
                        .map(this::toFotoDTO)
                        .collect(Collectors.toList()),
                item.getCriadoEm(),
                item.getAtualizadoEm()
        );
    }

    private FotoDTO toFotoDTO(FotoAcervo foto) {
        return new FotoDTO(
                null,                       // id (não usado)
                foto.getPublicId(),          // publicId
                foto.getVersion(),           // version
                foto.getLegenda(),           // legenda
                foto.isDestaque(),           // ehDestaque
                foto.getUrlVisualizacao(),   // url (fallback)
                foto.getNomeArquivo(),       // filename
                foto.getAutorNomePublico(),  // autorNomePublico
                foto.isLicenciamentoPermitido() // licenciamentoPermitido
        );
    }

    private void preencherDadosComuns(ItemAcervo item, ItemAcervoCreateDTO dto) {
        item.setTitulo(dto.titulo());
        item.setDescricao(dto.descricao());
        item.setLocal(dto.local());
        item.setDataOriginal(dto.dataOriginal());
        item.setProcedencia(dto.procedencia());
        item.setCreditoAutoral(dto.fotografoDoador());
        item.setTipo(dto.tipo());
        item.setModalidadeId(dto.modalidadeId());
        item.setAtletasIds(new ArrayList<>(dto.atletasIds()));
        item.setCuradorResponsavel(dto.curadorResponsavel());
        item.setRestricoesUso(dto.restricoesUso());
        item.setItemHistorico(Boolean.TRUE.equals(dto.itemHistorico()));

        if (Boolean.TRUE.equals(item.getItemHistorico())) {
            item.setStatus(StatusItemAcervo.MEMORIAL);
            item.setDisponivelParaLicenciamento(false);
            item.setPrecoBaseLicenciamento(BigDecimal.ZERO);
        } else {
            item.setStatus(dto.status() != null
                    ? dto.status()
                    : StatusItemAcervo.RASCUNHO);
            item.setDisponivelParaLicenciamento(dto.disponivelParaLicenciamento());
            item.setPrecoBaseLicenciamento(dto.precoBaseLicenciamento());
        }
    }
}
