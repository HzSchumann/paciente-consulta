package br.com.techchallenge.agendamento.service;

import br.com.techchallenge.agendamento.dto.AtualizarConsultaRequest;
import br.com.techchallenge.agendamento.dto.ConsultaResponse;
import br.com.techchallenge.agendamento.dto.CriarConsultaRequest;
import br.com.techchallenge.agendamento.entity.Consulta;
import br.com.techchallenge.agendamento.entity.ConsultaStatus;
import br.com.techchallenge.agendamento.mapper.ConsultaMapper;
import br.com.techchallenge.agendamento.repository.ConsultaRepository;
import br.com.techchallenge.contract.messaging.ConsultaEvent;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final OutboxService outboxService;
    private final AuditService auditService;
    private final ConsultaMapper consultaMapper;

    public ConsultaService(
            ConsultaRepository consultaRepository,
            OutboxService outboxService,
            AuditService auditService,
            ConsultaMapper consultaMapper
    ) {
        this.consultaRepository = consultaRepository;
        this.outboxService = outboxService;
        this.auditService = auditService;
        this.consultaMapper = consultaMapper;
    }

    @Transactional
    public ConsultaResponse criar(CriarConsultaRequest request) {
        Consulta consulta = new Consulta();
        consulta.setPacienteUsername(request.pacienteUsername());
        consulta.setMedicoUsername(request.medicoUsername());
        consulta.setEnfermeiroUsername(request.enfermeiroUsername());
        consulta.setDataHora(request.dataHora());
        consulta.setObservacoes(request.observacoes());
        consulta.setStatus(ConsultaStatus.AGENDADA);

        Consulta salva = consultaRepository.save(consulta);
        publicarEvento(salva, "CRIADA");
        auditService.registrar(request.medicoUsername(), "CONSULTA_CRIADA", "Consulta " + salva.getId() + " criada", true);
        return consultaMapper.toResponse(salva);
    }

    @Transactional
    public ConsultaResponse atualizar(Long id, AtualizarConsultaRequest request) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta não encontrada"));

        if (consulta.getStatus() == null) {
            consulta.setStatus(ConsultaStatus.AGENDADA);
        }

        if (request.dataHora() != null) {
            consulta.setDataHora(request.dataHora());
        }
        if (request.observacoes() != null) {
            consulta.setObservacoes(request.observacoes());
        }
        if (request.medicoUsername() != null) {
            consulta.setMedicoUsername(request.medicoUsername());
        }
        if (request.enfermeiroUsername() != null) {
            consulta.setEnfermeiroUsername(request.enfermeiroUsername());
        }

        Consulta salva = consultaRepository.save(consulta);
        publicarEvento(salva, "EDITADA");
        auditService.registrar(
                request.medicoUsername() != null ? request.medicoUsername() : "enfermeiro",
                "CONSULTA_EDITADA",
                "Consulta " + salva.getId() + " editada",
                true
        );
        return consultaMapper.toResponse(salva);
    }

    @Transactional
    public void cancelar(Long id, String actorUsername) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta não encontrada"));

        consulta.setStatus(ConsultaStatus.CANCELADA);
        Consulta salva = consultaRepository.save(consulta);
        publicarEvento(salva, "CANCELADA");
        auditService.registrar(actorUsername, "CONSULTA_CANCELADA", "Consulta " + salva.getId() + " cancelada", true);
    }

    private void publicarEvento(Consulta consulta, String acao) {
        ConsultaStatus status = consulta.getStatus() == null ? ConsultaStatus.AGENDADA : consulta.getStatus();
        outboxService.registrarConsultaEvento(new ConsultaEvent(
                "1.0",
                UUID.randomUUID().toString(),
                consulta.getId(),
                consulta.getPacienteUsername(),
                consulta.getMedicoUsername(),
                consulta.getEnfermeiroUsername(),
                consulta.getDataHora(),
                consulta.getObservacoes(),
                status.name(),
                acao,
                Instant.now()
        ));
    }
}
