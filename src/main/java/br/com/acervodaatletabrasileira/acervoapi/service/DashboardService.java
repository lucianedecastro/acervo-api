package br.com.acervodaatletabrasileira.acervoapi.service;

import br.com.acervodaatletabrasileira.acervoapi.dto.AdminDashboardStatsDTO;
import br.com.acervodaatletabrasileira.acervoapi.dto.AtletaDashboardStatsDTO;
import br.com.acervodaatletabrasileira.acervoapi.model.StatusItemAcervo;
import br.com.acervodaatletabrasileira.acervoapi.repository.AtletaRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ItemAcervoRepository;
import br.com.acervodaatletabrasileira.acervoapi.repository.ModalidadeRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final AtletaRepository atletaRepository;
    private final ItemAcervoRepository itemRepository;
    private final ModalidadeRepository modalidadeRepository;

    public DashboardService(AtletaRepository atletaRepository,
                            ItemAcervoRepository itemRepository,
                            ModalidadeRepository modalidadeRepository) {
        this.atletaRepository = atletaRepository;
        this.itemRepository = itemRepository;
        this.modalidadeRepository = modalidadeRepository;
    }

    /**
     * Visão Consolidada para Curadoria (ADMIN)
     */
    public Mono<AdminDashboardStatsDTO> getAdminStats() {
        return Mono.zip(
                atletaRepository.count(),
                itemRepository.count(),
                modalidadeRepository.count(),
                itemRepository.countByStatus(StatusItemAcervo.RASCUNHO),
                itemRepository.findAll().collectList()
        ).map(tuple -> {
            var itensPorTipo = tuple.getT5().stream()
                    .filter(item -> item.getTipo() != null)
                    .collect(Collectors.groupingBy(
                            item -> item.getTipo().name(),
                            Collectors.counting()
                    ));

            return new AdminDashboardStatsDTO(
                    tuple.getT1(), // totalAtletas
                    tuple.getT2(), // totalItens
                    tuple.getT3(), // totalModalidades
                    tuple.getT4(), // itensAguardandoPublicacao
                    itensPorTipo
            );
        });
    }

    /**
     * Visão Personalizada para a Atleta Logada
     * Filtra apenas itens onde o ID da atleta está presente na lista de donos.
     */
    public Mono<AtletaDashboardStatsDTO> getAtletaStats(String atletaId) {
        return Mono.zip(
                itemRepository.countByAtletasIdsContaining(atletaId),
                itemRepository.countByAtletasIdsContainingAndStatus(atletaId, StatusItemAcervo.PUBLICADO),
                itemRepository.countByAtletasIdsContainingAndStatus(atletaId, StatusItemAcervo.RASCUNHO),
                itemRepository.countByAtletasIdsContainingAndStatus(atletaId, StatusItemAcervo.MEMORIAL)
        ).map(tuple -> new AtletaDashboardStatsDTO(
                tuple.getT1(), // totalMeusItens
                tuple.getT2(), // itensPublicados
                tuple.getT3(), // itensEmRascunho
                tuple.getT4()  // itensNoMemorial
        ));
    }
}