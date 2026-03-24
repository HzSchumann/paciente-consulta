package br.com.techchallenge.agendamento.service;

import br.com.techchallenge.agendamento.dto.AtualizarConsultaRequest;
import br.com.techchallenge.agendamento.dto.ConsultaEvent;
import br.com.techchallenge.agendamento.dto.CriarConsultaRequest;
import br.com.techchallenge.agendamento.entity.Consulta;
import br.com.techchallenge.agendamento.messaging.ConsultaEventPublisher;
import br.com.techchallenge.agendamento.repository.ConsultaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final ConsultaEventPublisher eventPublisher;

    public ConsultaService(ConsultaRepository consultaRepository, ConsultaEventPublisher eventPublisher) {
        this.consultaRepository = consultaRepository;
        this.eventPublisher = eventPublisher;
    }

    public Consulta criar(CriarConsultaRequest request) {
        Consulta consulta = new Consulta();
        consulta.setPacienteUsername(request.pacienteUsername());
        consulta.setMedicoUsername(request.medicoUsername());
        consulta.setEnfermeiroUsername(request.enfermeiroUsername());
        consulta.setDataHora(request.dataHora());
        consulta.setObservacoes(request.observacoes());

        Consulta salva = consultaRepository.save(consulta);
        publicarEvento(salva, "CRIADA");
        return salva;
    }

    public Consulta atualizar(Long id, AtualizarConsultaRequest request) {
        Consulta consulta = consultaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta não encontrada"));

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
        return salva;
    }

    public List<Consulta> consultarHistoricoPaciente(String pacienteUsername, boolean somenteFuturas) {
        if (somenteFuturas) {
            return consultaRepository.findByPacienteUsernameAndDataHoraAfter(pacienteUsername, LocalDateTime.now());
        }
        return consultaRepository.findByPacienteUsername(pacienteUsername);
    }

    private void publicarEvento(Consulta consulta, String acao) {
        eventPublisher.publicar(new ConsultaEvent(
                consulta.getId(),
                consulta.getPacienteUsername(),
                consulta.getDataHora(),
                acao
        ));
    }
}
