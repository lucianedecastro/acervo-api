package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaPerfilDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaPublicoDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.SubcontaPagamentoDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.FotoPerfilAtleta;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import br.com.acervodaatletabrasileira.acervoapi.service.DocumentoLegalService;

@Service
public class AtletaService {

    private final AtletaRepository atletaRepository;
    private final ItemAcervoRepository acervoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AsaasService asaasService;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public AtletaService(
            AtletaRepository atletaRepository,
            ItemAcervoRepository acervoRepository,
            PasswordEncoder passwordEncoder,
            AsaasService asaasService
    ) {
        this.atletaRepository = atletaRepository;
        this.acervoRepository = acervoRepository;
        this.passwordEncoder = passwordEncoder;
        this.asaasService = asaasService;
    }

    /* ==========================
       BUSCA POR IDENTIDADE (DASHBOARD)
       ========================== */

    public Mono<Atleta> findByEmail(String email) {
        return atletaRepository.findByEmail(email);
    }

    /* ==========================
       BUSCA AGREGADA (PERFIL COMPLETO)
       ========================== */

    public Mono<AtletaPerfilDTO> getPerfilCompletoBySlug(String slug) {
        return atletaRepository.findBySlug(slug)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada com o slug: " + slug)))
                .flatMap(atleta ->
                        acervoRepository.findByAtletasIdsContaining(atleta.getId())
                                .collectList()
                                .map(itens -> new AtletaPerfilDTO(
                                        AtletaPublicoDTO.fromModel(atleta),
                                        itens
                                ))
                );
    }

    /* ==========================
       LEITURA (PÚBLICA)
       ========================== */

    public Flux<Atleta> findAll() {
        return atletaRepository.findAll();
    }

    public Mono<Atleta> findById(String id) {
        return atletaRepository.findById(id);
    }

    public Mono<Atleta> findBySlug(String slug) {
        return atletaRepository.findBySlug(slug);
    }

    /* ==========================
       CRIAÇÃO (ADMIN / CADASTRO)
       ========================== */

    public Mono<Atleta> create(AtletaFormDTO dto) {

        Atleta atleta = new Atleta();

        atleta.setNome(dto.nome());
        atleta.setNomeSocial(dto.nomeSocial());
        atleta.setSlug(generateSlug(dto.nome()));
        atleta.setCpf(dto.cpf());
        atleta.setEmail(dto.email());

        if (dto.senha() != null && !dto.senha().isBlank()) {
            atleta.setSenha(passwordEncoder.encode(dto.senha()));
        }

        atleta.setModalidadesIds(dto.modalidades());
        atleta.setBiografia(dto.biografia());
        atleta.setCategoria(dto.categoria() != null ? dto.categoria() : Atleta.CategoriaAtleta.ATIVA);

        atleta.setNomeRepresentante(dto.nomeRepresentante());
        atleta.setCpfRepresentante(dto.cpfRepresentante());
        atleta.setVinculoRepresentante(dto.vinculoRepresentante());

        atleta.setContratoAssinado(dto.contratoAssinado());
        atleta.setLinkContratoDigital(dto.linkContratoDigital());

        atleta.setDadosContato(dto.dadosContato());
        atleta.setTipoChavePix(dto.tipoChavePix());
        atleta.setChavePix(dto.chavePix());
        atleta.setBanco(dto.banco());
        atleta.setAgencia(dto.agencia());
        atleta.setConta(dto.conta());
        atleta.setTipoConta(dto.tipoConta());

        atleta.setComissaoPlataformaDiferenciada(dto.comissaoPlataformaDiferenciada());

        atleta.setFotoDestaqueUrl(dto.fotoDestaqueId());

        atleta.setCriadoEm(Instant.now());
        atleta.setAtualizadoEm(Instant.now());

        return atletaRepository.save(atleta);
    }

    /* ==========================
       ATUALIZAÇÃO (ADMIN)
       ========================== */

    public Mono<Atleta> update(String id, AtletaFormDTO dto) {

        return atletaRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada")))
                .flatMap(existente -> {

                    if (!existente.getNome().equalsIgnoreCase(dto.nome())) {
                        existente.setSlug(generateSlug(dto.nome()));
                    }

                    existente.setNome(dto.nome());
                    existente.setNomeSocial(dto.nomeSocial());

                    if (dto.senha() != null && !dto.senha().isBlank()) {
                        existente.setSenha(passwordEncoder.encode(dto.senha()));
                    }

                    existente.setModalidadesIds(dto.modalidades());
                    existente.setBiografia(dto.biografia());
                    existente.setCategoria(dto.categoria());

                    existente.setNomeRepresentante(dto.nomeRepresentante());
                    existente.setCpfRepresentante(dto.cpfRepresentante());
                    existente.setVinculoRepresentante(dto.vinculoRepresentante());

                    existente.setContratoAssinado(dto.contratoAssinado());
                    existente.setLinkContratoDigital(dto.linkContratoDigital());

                    existente.setDadosContato(dto.dadosContato());
                    existente.setTipoChavePix(dto.tipoChavePix());
                    existente.setChavePix(dto.chavePix());
                    existente.setBanco(dto.banco());
                    existente.setAgencia(dto.agencia());
                    existente.setConta(dto.conta());
                    existente.setTipoConta(dto.tipoConta());

                    existente.setComissaoPlataformaDiferenciada(dto.comissaoPlataformaDiferenciada());

                    existente.setFotoDestaqueUrl(dto.fotoDestaqueId());
                    existente.setStatusAtleta(dto.statusAtleta());

                    existente.setAtualizadoEm(Instant.now());

                    return atletaRepository.save(existente);
                });
    }

    /* ==========================
       FOTO DE PERFIL (AVATAR)
       ========================== */

    public Mono<Atleta> atualizarFotoPerfil(String atletaId, FotoPerfilAtleta fotoPerfil) {
        return atletaRepository.findById(atletaId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada")))
                .flatMap(atleta -> {
                    atleta.setFotoPerfil(fotoPerfil);
                    atleta.setAtualizadoEm(Instant.now());
                    return atletaRepository.save(atleta);
                });
    }

    /* ==========================
       FOTO DE DESTAQUE (HERO)
       ========================== */

    public Mono<Atleta> atualizarFotoDestaque(String atletaId, FotoPerfilAtleta fotoDestaque) {
        return atletaRepository.findById(atletaId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada")))
                .flatMap(atleta -> {
                    atleta.setFotoDestaque(fotoDestaque);
                    atleta.setAtualizadoEm(Instant.now());
                    return atletaRepository.save(atleta);
                });
    }

    /* ==========================
       VERIFICAÇÃO DE IDENTIDADE
       ========================== */

    public Mono<Atleta> verificarAtleta(String id, Atleta.StatusVerificacao novoStatus, String observacoes) {

        return atletaRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada")))
                .flatMap(atleta -> {

                    atleta.setStatusVerificacao(novoStatus);
                    atleta.setObservacoesAdmin(observacoes);
                    atleta.setDataVerificacao(Instant.now());
                    atleta.setAtualizadoEm(Instant.now());

                    if (novoStatus == Atleta.StatusVerificacao.VERIFICADO) {
                        atleta.setStatusAtleta("ATIVO");
                    }

                    return atletaRepository.save(atleta);
                });
    }

    /* ==========================
       PROVISIONAMENTO DA CONTA DE PAGAMENTO (ASAAS)
       ========================== */

    /**
     * Cria a subconta da atleta no gateway de pagamento e salva
     * o walletId retornado para uso no split das cobranças.
     *
     * Exige identidade já verificada (KYC interno da plataforma)
     * antes de avançar para o KYC do gateway.
     */
    public Mono<Atleta> provisionarContaPagamento(String id, SubcontaPagamentoDTO dto) {

        return atletaRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada")))
                .flatMap(atleta -> {

                    if (atleta.getStatusVerificacao() != Atleta.StatusVerificacao.VERIFICADO) {
                        return Mono.error(new IllegalStateException(
                                "Atleta precisa estar com identidade verificada antes de configurar a conta de recebimento"
                        ));
                    }

                    if (atleta.getGatewayAccountId() != null && !atleta.getGatewayAccountId().isBlank()) {
                        return Mono.error(new IllegalStateException(
                                "Atleta já possui conta de recebimento configurada"
                        ));
                    }

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("name", atleta.getNome());
                    payload.put("email", atleta.getEmail());
                    payload.put("cpfCnpj", atleta.getCpf());
                    payload.put("mobilePhone", dto.celular());
                    payload.put("address", dto.endereco());
                    payload.put("addressNumber", dto.numeroEndereco());
                    payload.put("province", dto.bairro());
                    payload.put("postalCode", dto.cep());
                    payload.put("incomeValue", dto.rendaMensal());

                    if (dto.dataNascimento() != null) {
                        payload.put("birthDate", dto.dataNascimento().toString());
                    }

                    return asaasService.criarSubcontaAtleta(payload)
                            .flatMap(resposta -> {

                                Object walletId = resposta.get("walletId");
                                if (walletId == null) {
                                    return Mono.error(new IllegalStateException(
                                            "Asaas não retornou walletId para a subconta criada"
                                    ));
                                }

                                atleta.setGatewayAccountId(walletId.toString());
                                atleta.setAtualizadoEm(Instant.now());

                                return atletaRepository.save(atleta);
                            });
                });
    }

    /* ==========================
       VINCULAÇÃO DE DOCUMENTOS LEGAIS
       ========================== */

    public Mono<Atleta> vincularDocumentoLegal(
            String id,
            DocumentoLegalService.MetadataDocumentoLegal metadata,
            DocumentoLegalService.TipoDocumento tipo
    ) {

        return atletaRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada")))
                .flatMap(atleta -> {

                    if (tipo == DocumentoLegalService.TipoDocumento.CONTRATO) {
                        atleta.setLinkContratoDigital(metadata.getUrl());
                        atleta.setContratoGestaoHash(metadata.getHashIntegridade());
                        atleta.setContratoAssinado(true);
                        atleta.setDataAssinaturaContrato(Instant.now());
                    } else {
                        atleta.setDocumentoIdentidadeUrl(metadata.getUrl());
                        atleta.setScoreValidacaoDocumento("AGUARDANDO_ANALISE");
                    }

                    atleta.setAtualizadoEm(Instant.now());

                    return atletaRepository.save(atleta);
                });
    }

    /* ==========================
       UTIL (GERADOR DE SLUG)
       ========================== */

    public Mono<Void> deleteById(String id) {
        return atletaRepository.deleteById(id);
    }

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