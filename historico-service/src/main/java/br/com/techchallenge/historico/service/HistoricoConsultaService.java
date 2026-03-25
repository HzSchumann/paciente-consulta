package br.com.techchallenge.historico.service;

import br.com.techchallenge.contract.messaging.ConsultaEvent;
import br.com.techchallenge.historico.dto.ConsultaHistoricoGraphQlType;
import br.com.techchallenge.historico.entity.ConsultaHistoricoProjection;
import br.com.techchallenge.historico.entity.ProcessedHistoricoEvent;
import br.com.techchallenge.historico.repository.ConsultaHistoricoProjectionRepository;
import br.com.techchallenge.historico.repository.ProcessedHistoricoEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class HistoricoConsultaService {

    private final ConsultaHistoricoProjectionRepository repository;
    private final ProcessedHistoricoEventRepository processedHistoricoEventRepository;

    public HistoricoConsultaService(
            ConsultaHistoricoProjectionRepository repository,
            ProcessedHistoricoEventRepository processedHistoricoEventRepository
    ) {
        this.repository = repository;
        this.processedHistoricoEventRepository = processedHistoricoEventRepository;
    }

    @Transactional(readOnly = true)
    public List<ConsultaHistoricoGraphQlType> consultarHistoricoPaciente(String pacienteUsername, boolean somenteFuturas) {
        List<ConsultaHistoricoProjection> projections = somenteFuturas
                ? repository.findByPacienteUsernameAndStatusNotAndDataHoraAfterOrderByDataHoraAsc(
                        pacienteUsername,
                        "CANCELADA",
                        LocalDateTime.now()
                )
                : repository.findByPacienteUsernameOrderByDataHoraAsc(pacienteUsername);

        return projections.stream()
                .map(this::toGraphQlType)
                .toList();
    }

    @Transactional
    public void projetar(ConsultaEvent event) {
        if (processedHistoricoEventRepository.existsById(event.eventId())) {
            return;
        }
        ConsultaHistoricoProjection projection = repository.findById(event.consultaId())
                .orElseGet(ConsultaHistoricoProjection::new);
        projection.setId(event.consultaId());
        projection.setPacienteUsername(event.pacienteUsername());
        projection.setMedicoUsername(event.medicoUsername());
        projection.setEnfermeiroUsername(event.enfermeiroUsername());
        projection.setDataHora(event.dataHora());
        projection.setObservacoes(event.observacoes());
        projection.setStatus(event.status());
        projection.setUltimaAtualizacaoEm(LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC));
        repository.save(projection);
        processedHistoricoEventRepository.save(new ProcessedHistoricoEvent(event.eventId()));
    }

    private ConsultaHistoricoGraphQlType toGraphQlType(ConsultaHistoricoProjection projection) {
        return new ConsultaHistoricoGraphQlType(
                projection.getId(),
                projection.getPacienteUsername(),
                projection.getMedicoUsername(),
                projection.getEnfermeiroUsername(),
                projection.getDataHora(),
                projection.getObservacoes(),
                projection.getStatus(),
                projection.getUltimaAtualizacaoEm()
        );
    }
}
