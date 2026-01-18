package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaFormDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.Atleta;
import br.com.acervodaatletabrasileira.acervoapi.model.ItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;

@Service
public class AtletaService {

    private final AtletaRepository atletaRepository;

    public AtletaService(AtletaRepository atletaRepository) {
        this.atletaRepository = atletaRepository;
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

    /* ==========================
       CRIAÇÃO (ADMIN)
       ========================== */

    public Mono<Atleta> create(AtletaFormDTO dto) {
        Atleta atleta = new Atleta();

        // Mapeamento dos novos campos de identidade e biografia
        atleta.setNome(dto.nome());
        atleta.setNomeSocial(dto.nomeSocial());
        atleta.setModalidades(dto.modalidades()); // Agora suporta a lista de trajetórias
        atleta.setBiografia(dto.biografia());

        // Mapeamento dos pilares Jurídico e Financeiro
        atleta.setContratoAssinado(dto.contratoAssinado());
        atleta.setLinkContratoDigital(dto.linkContratoDigital());
        atleta.setDadosContato(dto.dadosContato());
        atleta.setInformacoesParaRepasse(dto.informacoesParaRepasse());
        atleta.setComissaoPlataformaDiferenciada(dto.comissaoPlataformaDiferenciada());

        // Inicialização do Acervo
        atleta.setItens(new ArrayList<>());
        atleta.setFotoDestaqueId(dto.fotoDestaqueId());
        atleta.setStatusAtleta(dto.statusAtleta() != null ? dto.statusAtleta() : "AGUARDANDO_CONTRATO");

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
                .flatMap(atletaExistente -> {
                    // Atualiza campos biográficos
                    atletaExistente.setNome(dto.nome());
                    atletaExistente.setNomeSocial(dto.nomeSocial());
                    atletaExistente.setModalidades(dto.modalidades());
                    atletaExistente.setBiografia(dto.biografia());

                    // Atualiza campos de governança e finanças
                    atletaExistente.setContratoAssinado(dto.contratoAssinado());
                    atletaExistente.setLinkContratoDigital(dto.linkContratoDigital());
                    atletaExistente.setDadosContato(dto.dadosContato());
                    atletaExistente.setInformacoesParaRepasse(dto.informacoesParaRepasse());
                    atletaExistente.setComissaoPlataformaDiferenciada(dto.comissaoPlataformaDiferenciada());

                    atletaExistente.setFotoDestaqueId(dto.fotoDestaqueId());
                    atletaExistente.setStatusAtleta(dto.statusAtleta());
                    atletaExistente.setAtualizadoEm(Instant.now());

                    return atletaRepository.save(atletaExistente);
                });
    }

    /* ==========================
       DELETE (ADMIN)
       ========================== */

    public Mono<Void> deleteById(String id) {
        // No futuro, podemos adicionar uma trava aqui para não deletar
        // atletas que possuam transações financeiras ativas.
        return atletaRepository.deleteById(id);
    }
}