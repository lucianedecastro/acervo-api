package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoCreateDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.ItemAcervoResponseDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusBlockchain;
import br.com.acervodaatletabrasileira.acervoapi.model.TipoDecisao;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.*;

@Service
@Slf4j
public class ItemAcervoService {

    private final ItemAcervoRepository repository;
    private final AtletaRepository atletaRepository;
    private final CloudinaryService cloudinaryService;
    private final Cloudinary cloudinary;
    private final BlockchainService blockchainService;
    private final GovernancaService governancaService;

    private static final List<StatusItemAcervo> STATUS_PUBLICOS = List.of(
            StatusItemAcervo.PUBLICADO,
            StatusItemAcervo.DISPONIVEL_LICENCIAMENTO,
            StatusItemAcervo.MEMORIAL
    );

    public ItemAcervoService(
            ItemAcervoRepository repository,
            AtletaRepository atletaRepository,
            CloudinaryService cloudinaryService,
            Cloudinary cloudinary,
            BlockchainService blockchainService,
            GovernancaService governancaService
    ) {
        this.repository = repository;
        this.atletaRepository = atletaRepository;
        this.cloudinaryService = cloudinaryService;
        this.cloudinary = cloudinary;
        this.blockchainService = blockchainService;
        this.governancaService = governancaService;
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
       CONSULTAS ADMIN (SEM FILTRO DE STATUS)
       ===================================================== */

    /**
     * Busca um item por ID para contexto administrativo.
     * Diferente de buscarPublicadoPorId, não filtra por status —
     * necessário para curadoria de itens em RASCUNHO/ARQUIVADO.
     */
    public Mono<ItemAcervoResponseDTO> buscarPorIdAdmin(String id) {
        return repository.findById(id)
                .map(this::toResponseDTO);
    }

    /* =====================================================
       CRIAÇÃO
       ===================================================== */

    public Mono<ItemAcervo> criar(ItemAcervoCreateDTO dto) {

        if (dto.tipo() == null)
            return Mono.error(new IllegalArgumentException("Tipo do item é obrigatório"));

        if (dto.modalidadeId() == null || dto.modalidadeId().isBlank())
            return Mono.error(new IllegalArgumentException("Modalidade é obrigatória"));

        if (dto.atletasIds() == null || dto.atletasIds().isEmpty())
            return Mono.error(new IllegalArgumentException("Item deve ter ao menos uma atleta vinculada"));

        ItemAcervo item = new ItemAcervo();
        preencherDadosComuns(item, dto);

        item.setStatusBlockchain(StatusBlockchain.NAO_REGISTRADO);
        item.setCriadoEm(Instant.now());
        item.setAtualizadoEm(Instant.now());

        return repository.save(item);
    }

    /* =====================================================
       ATUALIZAÇÃO
       ===================================================== */

    /**
     * Atualiza os dados editoriais de um item já existente.
     * Não altera fotos, hash/status de blockchain ou data de criação —
     * esses fluxos têm seus próprios endpoints dedicados.
     */
    public Mono<ItemAcervo> atualizar(String id, ItemAcervoCreateDTO dto) {

        if (dto.tipo() == null)
            return Mono.error(new IllegalArgumentException("Tipo do item é obrigatório"));

        if (dto.modalidadeId() == null || dto.modalidadeId().isBlank())
            return Mono.error(new IllegalArgumentException("Modalidade é obrigatória"));

        if (dto.atletasIds() == null || dto.atletasIds().isEmpty())
            return Mono.error(new IllegalArgumentException("Item deve manter ao menos uma atleta vinculada"));

        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                .flatMap(item -> {
                    preencherDadosComuns(item, dto);
                    item.setAtualizadoEm(Instant.now());
                    return repository.save(item);
                });
    }

    /* =====================================================
       REMOÇÃO
       ===================================================== */

    public Mono<Void> deletar(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                .flatMap(repository::delete);
    }

    /* =====================================================
       PUBLICAÇÃO
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

    /* =====================================================
       CARIMBO INSTITUCIONAL (BLOCKCHAIN)
       ===================================================== */

    /**
     * Registra na Blockchain o hash de integridade já calculado
     * no upload da foto principal (blockchainContentHash).
     *
     * Ato administrativo separado da publicação, como já
     * documentado nas diretrizes deste módulo.
     */
    public Mono<ItemAcervo> registrarSeloBlockchain(String id, String responsavel) {

        return repository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                .flatMap(item -> {

                    if (item.getBlockchainContentHash() == null || item.getBlockchainContentHash().isBlank()) {
                        return Mono.error(new IllegalStateException(
                                "Item não possui hash de integridade gerado (nenhuma foto de destaque enviada ainda)"
                        ));
                    }

                    if (item.getStatusBlockchain() == StatusBlockchain.REGISTRADO) {
                        return Mono.error(new IllegalStateException(
                                "Item já possui selo institucional registrado"
                        ));
                    }

                    return blockchainService.registrarProvaInstitucional(
                                    "ITEM_ACERVO", item.getId(), item.getBlockchainContentHash()
                            )
                            .flatMap(txHash -> {
                                item.setBlockchainTxId(txHash);
                                item.setDataRegistroBlockchain(Instant.now());
                                item.setStatusBlockchain(StatusBlockchain.REGISTRADO);
                                item.setAtualizadoEm(Instant.now());
                                return repository.save(item);
                            })
                            .flatMap(itemSalvo ->
                                    governancaService.registrarDecisao(
                                            TipoDecisao.ADMINISTRATIVA,
                                            "ITEM_ACERVO",
                                            itemSalvo.getId(),
                                            "SELO_INSTITUCIONAL_REGISTRADO",
                                            "Item do acervo registrado na blockchain. TxId: " + itemSalvo.getBlockchainTxId(),
                                            responsavel,
                                            "ROLE_ADMIN"
                                    ).thenReturn(itemSalvo)
                            )
                            .onErrorResume(erro -> {
                                log.error("Falha ao registrar selo institucional do item {}", id, erro);
                                item.setStatusBlockchain(StatusBlockchain.FALHA_REGISTRO);
                                item.setAtualizadoEm(Instant.now());
                                return repository.save(item).then(Mono.error(erro));
                            });
                });
    }

    /* =====================================================
       UPLOAD COM HASH INSTITUCIONAL
       ===================================================== */

    public Mono<FotoDTO> adicionarFoto(String itemId, FilePart filePart, FotoDTO metadata) {

        return repository.findById(itemId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item não encontrado")))
                .flatMap(item ->

                        Mono.fromCallable(() -> Files.createTempFile("img_" + itemId, ".tmp"))

                                .flatMap(tempPath ->

                                        filePart.transferTo(tempPath)
                                                .then(Mono.fromCallable(() -> {

                                                    File file = tempPath.toFile();
                                                    String hash = calcularHashSHA256(file);

                                                    Map uploadResult = cloudinary.uploader().upload(file,
                                                            ObjectUtils.asMap(
                                                                    "folder", "acervo/itens",
                                                                    "public_id", UUID.randomUUID().toString()
                                                            )
                                                    );

                                                    return new UploadComHash(uploadResult, hash, tempPath);

                                                }).subscribeOn(Schedulers.boundedElastic()))

                                                .doFinally(signal -> {
                                                    try { Files.deleteIfExists(tempPath); }
                                                    catch (Exception ignored) {}
                                                })
                                )

                                .flatMap((UploadComHash uploadData) -> {

                                    Map result = uploadData.getResult();
                                    String hash = uploadData.getHash();

                                    FotoAcervo foto = new FotoAcervo();
                                    foto.setPublicId((String) result.get("public_id"));

                                    Object versionObj = result.get("version");
                                    foto.setVersion(versionObj instanceof Integer
                                            ? ((Integer) versionObj).longValue()
                                            : (Long) versionObj);

                                    foto.setLegenda(metadata.legenda());
                                    foto.setDestaque(Boolean.TRUE.equals(metadata.ehDestaque()));
                                    foto.setAutorNomePublico(metadata.autorNomePublico());
                                    foto.setLicenciamentoPermitido(Boolean.TRUE.equals(metadata.licenciamentoPermitido()));
                                    foto.setPossuiMarcaDagua(true);

                                    if (item.getFotos() == null)
                                        item.setFotos(new ArrayList<>());

                                    if (Boolean.TRUE.equals(metadata.ehDestaque()) || item.getFotos().isEmpty()) {
                                        item.setBlockchainContentHash(hash);
                                        item.setStatusBlockchain(StatusBlockchain.NAO_REGISTRADO);
                                    }

                                    item.getFotos().add(foto);
                                    item.setAtualizadoEm(Instant.now());

                                    return repository.save(item)
                                            .thenReturn(toFotoDTO(foto));
                                })
                );
    }

    /* =====================================================
       HASH UTILITÁRIO
       ===================================================== */

    private String calcularHashSHA256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /* =====================================================
       MAPEAMENTOS
       ===================================================== */

    private FotoDTO toFotoDTO(FotoAcervo foto) {
        return new FotoDTO(
                null,
                foto.getPublicId(),
                foto.getVersion(),
                foto.getLegenda(),
                foto.isDestaque(),
                null,
                null,
                foto.getAutorNomePublico(),
                foto.isLicenciamentoPermitido()
        );
    }

    private ItemAcervoResponseDTO toResponseDTO(ItemAcervo item) {
        return new ItemAcervoResponseDTO(
                item.getId(),
                item.getTitulo(),
                item.getDescricao(),
                item.getLocal(),
                item.getDataOriginal(),
                item.getProcedencia(),
                item.getCreditoAutoral(),
                item.getFontePesquisa(),
                item.getLinkFontePesquisa(),
                item.getDominioPublico(),
                item.getBlockchainTxId(),
                item.getDataRegistroBlockchain(),
                item.getTipo(),
                item.getStatus(),
                item.getPrecoBaseLicenciamento(),
                item.getDisponivelParaLicenciamento(),
                item.getItemHistorico(),
                item.getModalidadeId(),
                item.getAtletasIds(),
                item.getFotos() == null ? List.of()
                        : item.getFotos().stream().map(this::toFotoDTO).toList(),
                item.getCriadoEm(),
                item.getAtualizadoEm()
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
        item.setFontePesquisa(dto.fontePesquisa());
        item.setLinkFontePesquisa(dto.linkFontePesquisa());
        item.setDominioPublico(Boolean.TRUE.equals(dto.dominioPublico()));

        if (Boolean.TRUE.equals(item.getItemHistorico())) {
            item.setStatus(StatusItemAcervo.MEMORIAL);
            item.setDisponivelParaLicenciamento(false);
            item.setPrecoBaseLicenciamento(BigDecimal.ZERO);
        } else {
            item.setStatus(dto.status() != null ? dto.status() : StatusItemAcervo.RASCUNHO);
            item.setDisponivelParaLicenciamento(dto.disponivelParaLicenciamento());
            item.setPrecoBaseLicenciamento(dto.precoBaseLicenciamento());
        }
    }

    /* =====================================================
       CLASSE INTERNA AUXILIAR
       ===================================================== */

    private static class UploadComHash {
        private final Map result;
        private final String hash;
        private final Path tempPath;

        public UploadComHash(Map result, String hash, Path tempPath) {
            this.result = result;
            this.hash = hash;
            this.tempPath = tempPath;
        }

        public Map getResult() { return result; }
        public String getHash() { return hash; }
        public Path getTempPath() { return tempPath; }
    }
}