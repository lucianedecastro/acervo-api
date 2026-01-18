package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoCreateDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ItemAcervoService {

    private final ItemAcervoRepository repository;
    private final CloudinaryService cloudinaryService;

    // Lista de status considerados "Públicos" para facilitar a manutenção
    private final List<StatusItemAcervo> STATUS_PUBLICOS = Arrays.asList(
            StatusItemAcervo.PUBLICADO,
            StatusItemAcervo.DISPONIVEL_LICENCIAMENTO,
            StatusItemAcervo.MEMORIAL
    );

    public ItemAcervoService(ItemAcervoRepository repository, CloudinaryService cloudinaryService) {
        this.repository = repository;
        this.cloudinaryService = cloudinaryService;
    }

    /* =====================================================
       CONSULTAS PÚBLICAS (OTIMIZADAS)
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
        return repository.findByAtletasIdsContainingAndStatusIn(atletaId, STATUS_PUBLICOS)
                .map(this::toResponseDTO);
    }

    public Flux<ItemAcervoResponseDTO> listarPublicadosPorModalidade(String modalidadeId) {
        return repository.findByModalidadeIdAndStatus(modalidadeId, StatusItemAcervo.PUBLICADO) // Simplificado: use findByModalidadeIdAndStatusIn se necessário
                .map(this::toResponseDTO);
    }

    /* =====================================================
       CURADORIA / ADMIN (COM VALIDAÇÃO DE PROPRIEDADE)
       ===================================================== */

    public Flux<ItemAcervo> listarTodos() {
        return repository.findAll();
    }

    public Mono<ItemAcervo> criar(ItemAcervoCreateDTO dto) {
        ItemAcervo item = new ItemAcervo();
        preencherDadosComuns(item, dto);
        item.setCriadoEm(Instant.now());
        item.setAtualizadoEm(Instant.now());
        return repository.save(item);
    }

    /**
     * Atualização com Trava de Segurança:
     * Se quem estiver editando for uma ATLETA, ela só consegue se o ID dela estiver na lista.
     * Se for ADMIN, o acesso é liberado.
     */
    public Mono<ItemAcervo> atualizarProtegido(String id, ItemAcervoCreateDTO dto, String usuarioId, String role) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                .flatMap(existente -> {
                    // Se for Atleta, verifica se ela é "dona" do item
                    if ("ROLE_ATLETA".equals(role) && !existente.getAtletasIds().contains(usuarioId)) {
                        return Mono.error(new AccessDeniedException("Você não tem permissão para editar este item."));
                    }
                    preencherDadosComuns(existente, dto);
                    existente.setAtualizadoEm(Instant.now());
                    return repository.save(existente);
                });
    }

    // Mantido para compatibilidade com Admin legado ou uso interno
    public Mono<ItemAcervo> atualizar(String id, ItemAcervoCreateDTO dto) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                .flatMap(existente -> {
                    preencherDadosComuns(existente, dto);
                    existente.setAtualizadoEm(Instant.now());
                    return repository.save(existente);
                });
    }

    private void preencherDadosComuns(ItemAcervo item, ItemAcervoCreateDTO dto) {
        item.setTitulo(dto.titulo());
        item.setDescricao(dto.descricao());
        item.setLocal(dto.local());
        item.setDataOriginal(dto.dataOriginal());
        item.setProcedencia(dto.procedencia());
        item.setFotografoDoador(dto.fotografoDoador());
        item.setTipo(dto.tipo());
        item.setModalidadeId(dto.modalidadeId());
        item.setAtletasIds(dto.atletasIds());
        item.setCuradorResponsavel(dto.curadorResponsavel());
        item.setRestricoesUso(dto.restricoesUso());
        item.setFotos(mapFotos(dto.fotos()));
        item.setItemHistorico(Boolean.TRUE.equals(dto.itemHistorico()));

        if (item.getItemHistorico()) {
            item.setStatus(StatusItemAcervo.MEMORIAL);
            item.setDisponivelParaLicenciamento(false);
            item.setPrecoBaseLicenciamento(BigDecimal.ZERO);
        } else {
            item.setStatus(dto.status() != null ? dto.status() : StatusItemAcervo.RASCUNHO);
            item.setDisponivelParaLicenciamento(dto.disponivelParaLicenciamento());
            item.setPrecoBaseLicenciamento(dto.precoBaseLicenciamento());
        }
    }

    public Mono<ItemAcervo> publicar(String id) {
        return repository.findById(id)
                .flatMap(item -> {
                    item.setStatus(Boolean.TRUE.equals(item.getItemHistorico()) ?
                            StatusItemAcervo.MEMORIAL : StatusItemAcervo.PUBLICADO);
                    item.setAtualizadoEm(Instant.now());
                    return repository.save(item);
                });
    }

    public Mono<Void> remover(String id) {
        return repository.deleteById(id);
    }

    /* =====================================================
       UPLOAD DE MÍDIA E MAPPERS (Mantidos)
       ===================================================== */
    // ... (restante do código igual ao original para não estender demais o arquivo)

    public Mono<FotoDTO> uploadCloudinaryPuro(FilePart file) {
        return cloudinaryService.uploadImagem(file, "acervo/temp")
                .map(result -> FotoDTO.fromUpload(result.get("url"), result.get("publicId"), "Upload avulso", false));
    }

    public Mono<FotoDTO> adicionarFoto(String itemId, FilePart file, FotoDTO metadata) {
        return repository.findById(itemId)
                .flatMap(item -> cloudinaryService.uploadImagem(file, "acervo")
                        .flatMap(result -> {
                            FotoAcervo foto = new FotoAcervo();
                            foto.setPublicId(result.get("publicId"));
                            foto.setUrlVisualizacao(result.get("url"));
                            foto.setLegenda(metadata.legenda());
                            foto.setDestaque(Boolean.TRUE.equals(metadata.ehDestaque()));
                            if (item.getFotos() == null) item.setFotos(new ArrayList<>());
                            item.getFotos().add(foto);
                            item.setAtualizadoEm(Instant.now());
                            return repository.save(item).thenReturn(toFotoDTO(foto));
                        }));
    }

    private ItemAcervoResponseDTO toResponseDTO(ItemAcervo item) {
        return new ItemAcervoResponseDTO(item.getId(), item.getTitulo(), item.getDescricao(), item.getLocal(), item.getDataOriginal(), item.getProcedencia(), item.getTipo(), item.getStatus(), item.getPrecoBaseLicenciamento(), item.getDisponivelParaLicenciamento(), item.getItemHistorico(), item.getModalidadeId(), item.getAtletasIds(), item.getFotos() == null ? List.of() : item.getFotos().stream().map(this::toFotoDTO).toList(), item.getCriadoEm(), item.getAtualizadoEm());
    }

    private FotoDTO toFotoDTO(FotoAcervo foto) {
        return new FotoDTO(null, foto.getPublicId(), foto.getLegenda(), foto.isDestaque(), foto.getUrlVisualizacao(), null);
    }

    private List<FotoAcervo> mapFotos(List<FotoDTO> fotos) {
        if (fotos == null) return List.of();
        return fotos.stream().map(dto -> {
            FotoAcervo foto = new FotoAcervo();
            foto.setPublicId(dto.publicId() != null ? dto.publicId() : UUID.randomUUID().toString());
            foto.setUrlVisualizacao(dto.url());
            foto.setLegenda(dto.legenda());
            foto.setDestaque(Boolean.TRUE.equals(dto.ehDestaque()));
            return foto;
        }).toList();
    }
}