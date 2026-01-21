package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.FotografaPerfilDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Fotografa;
import br.com.acervodaatletabrasileira.acervoapi.repository.FotografaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class FotografaService {

    private final FotografaRepository fotografaRepository;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public FotografaService(FotografaRepository fotografaRepository,
                            PasswordEncoder passwordEncoder) {
        this.fotografaRepository = fotografaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /* ==========================
       BUSCA POR IDENTIDADE (DASHBOARD)
       ========================== */

    public Mono<Fotografa> findByEmail(String email) {
        return fotografaRepository.findByEmail(email);
    }

    public Mono<Fotografa> findById(String id) {
        return fotografaRepository.findById(id);
    }

    /* ==========================
       LEITURA PÚBLICA
       ========================== */

    /**
     * Lista apenas fotógrafas públicas
     * (ativas, espólio ou memorial)
     */
    public Flux<Fotografa> findAllPublicas() {
        return fotografaRepository.findAll()
                .filter(f ->
                        f.getStatusFotografa() != Fotografa.StatusFotografa.BLOQUEADA
                );
    }

    public Mono<Fotografa> findPublicaById(String id) {
        return fotografaRepository.findById(id)
                .filter(f -> f.getStatusFotografa() != Fotografa.StatusFotografa.BLOQUEADA);
    }

    public Mono<Fotografa> findPublicaBySlug(String slug) {
        return fotografaRepository.findBySlug(slug)
                .filter(f -> f.getStatusFotografa() != Fotografa.StatusFotografa.BLOQUEADA);
    }

    /* ==========================
       PERFIL COMPLETO (SLUG)
       ========================== */

    /**
     * Retorna o perfil público completo da fotógrafa.
     * A coleção poderá ser agregada aqui futuramente.
     */
    public Mono<FotografaPerfilDTO> getPerfilCompletoBySlug(String slug) {
        return fotografaRepository.findBySlug(slug)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("Fotógrafa não encontrada com slug: " + slug)
                ))
                .map(FotografaPerfilDTO::fromModel);
    }

    /* ==========================
       CADASTRO (AUTOCADASTRO)
       ========================== */

    public Mono<Fotografa> createCadastro(Fotografa fotografa) {

        fotografa.setId(null);
        fotografa.setSlug(generateSlug(fotografa.getNome()));
        fotografa.setStatusVerificacao(Fotografa.StatusVerificacao.PENDENTE);
        fotografa.setStatusFotografa(Fotografa.StatusFotografa.INATIVA);
        fotografa.setContratoAssinado(false);

        if (fotografa.getSenha() != null && !fotografa.getSenha().isBlank()) {
            fotografa.setSenha(passwordEncoder.encode(fotografa.getSenha()));
        }

        fotografa.setCriadoEm(Instant.now());
        fotografa.setAtualizadoEm(Instant.now());

        return fotografaRepository.save(fotografa);
    }

    /* ==========================
       ATUALIZAÇÃO (ADMIN)
       ========================== */

    public Mono<Fotografa> update(String id, Fotografa atualizada) {
        return fotografaRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Fotógrafa não encontrada")))
                .flatMap(existente -> {

                    if (!existente.getNome().equalsIgnoreCase(atualizada.getNome())) {
                        existente.setSlug(generateSlug(atualizada.getNome()));
                    }

                    existente.setNome(atualizada.getNome());
                    existente.setNomeSocial(atualizada.getNomeSocial());
                    existente.setCpf(atualizada.getCpf());
                    existente.setEmail(atualizada.getEmail());

                    if (atualizada.getSenha() != null && !atualizada.getSenha().isBlank()) {
                        existente.setSenha(passwordEncoder.encode(atualizada.getSenha()));
                    }

                    existente.setBiografia(atualizada.getBiografia());
                    existente.setRegistroProfissional(atualizada.getRegistroProfissional());
                    existente.setLinkRedeSocialPrincipal(atualizada.getLinkRedeSocialPrincipal());
                    existente.setFotosDestaque(atualizada.getFotosDestaque());
                    existente.setLinkColecaoCompleta(atualizada.getLinkColecaoCompleta());

                    existente.setCategoria(atualizada.getCategoria());

                    existente.setNomeRepresentante(atualizada.getNomeRepresentante());
                    existente.setCpfRepresentante(atualizada.getCpfRepresentante());
                    existente.setVinculoRepresentante(atualizada.getVinculoRepresentante());

                    existente.setContratoAssinado(atualizada.getContratoAssinado());
                    existente.setLinkContratoDigital(atualizada.getLinkContratoDigital());
                    existente.setDataAssinaturaContrato(atualizada.getDataAssinaturaContrato());

                    existente.setTipoChavePix(atualizada.getTipoChavePix());
                    existente.setChavePix(atualizada.getChavePix());
                    existente.setBanco(atualizada.getBanco());
                    existente.setAgencia(atualizada.getAgencia());
                    existente.setConta(atualizada.getConta());
                    existente.setTipoConta(atualizada.getTipoConta());

                    existente.setPercentualRepasse(atualizada.getPercentualRepasse());
                    existente.setComissaoPlataformaDiferenciada(atualizada.getComissaoPlataformaDiferenciada());

                    existente.setStatusFotografa(atualizada.getStatusFotografa());
                    existente.setAtualizadoEm(Instant.now());

                    return fotografaRepository.save(existente);
                });
    }

    /* ==========================
       VERIFICAÇÃO ADMINISTRATIVA
       ========================== */

    public Mono<Fotografa> verificarFotografa(
            String id,
            Fotografa.StatusVerificacao novoStatus,
            String observacoes
    ) {
        return fotografaRepository.findById(id)
                .flatMap(f -> {
                    f.setStatusVerificacao(novoStatus);
                    f.setObservacoesAdmin(observacoes);
                    f.setDataVerificacao(Instant.now());
                    f.setAtualizadoEm(Instant.now());

                    if (novoStatus == Fotografa.StatusVerificacao.VERIFICADO) {
                        f.setStatusFotografa(Fotografa.StatusFotografa.ATIVA);
                    }

                    return fotografaRepository.save(f);
                });
    }

    /* ==========================
       REMOÇÃO
       ========================== */

    public Mono<Void> deleteById(String id) {
        return fotografaRepository.deleteById(id);
    }

    /* ==========================
       UTIL – GERADOR DE SLUG
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
