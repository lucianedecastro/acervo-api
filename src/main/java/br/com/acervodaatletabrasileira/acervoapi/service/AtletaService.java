package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaPerfilDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaPublicoDTO;
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
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AtletaService {

    private final AtletaRepository atletaRepository;
    private final ItemAcervoRepository acervoRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public AtletaService(AtletaRepository atletaRepository,
                         ItemAcervoRepository acervoRepository,
                         PasswordEncoder passwordEncoder) {
        this.atletaRepository = atletaRepository;
        this.acervoRepository = acervoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /* ==========================
       BUSCA POR IDENTIDADE (DASHBOARD)
       ========================== */

    public Mono<Atleta> findByEmail(String email) {
        return atletaRepository.findByEmail(email);
    }

    /* ==========================
       BUSCA AGREGADA (O COMBO - PROTEGIDO LGPD)
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

                    // CAMPOS SENSÍVEIS NÃO DEVEM SER ALTERADOS PELO ADMIN
                    // email e cpf são mantidos como estão

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
       FOTO DE PERFIL (NOVO – SEM QUEBRA)
       ========================== */

    public Mono<Atleta> atualizarFotoPerfil(String atletaId, FotoPerfilAtleta fotoPerfil) {
        return atletaRepository.findById(atletaId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Atleta não encontrada")))
                .flatMap(atleta -> {
                    atleta.setFotoPerfil(fotoPerfil);
                    atleta.setFotoDestaqueUrl(fotoPerfil.getUrl());
                    atleta.setAtualizadoEm(Instant.now());
                    return atletaRepository.save(atleta);
                });
    }

    /* ==========================
       VERIFICAÇÃO DE IDENTIDADE
       ========================== */

    public Mono<Atleta> verificarAtleta(String id, Atleta.StatusVerificacao novoStatus, String observacoes) {
        return atletaRepository.findById(id)
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

    public Mono<Void> deleteById(String id) {
        return atletaRepository.deleteById(id);
    }

    /* ==========================
       UTIL (Gerador de Slug)
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
