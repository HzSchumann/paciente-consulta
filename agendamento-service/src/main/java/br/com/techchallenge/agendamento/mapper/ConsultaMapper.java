package br.com.techchallenge.agendamento.mapper;

import br.com.techchallenge.agendamento.dto.ConsultaGraphQlType;
import br.com.techchallenge.agendamento.dto.ConsultaResponse;
import br.com.techchallenge.agendamento.entity.Consulta;
import org.springframework.stereotype.Component;

@Component
public class ConsultaMapper {

    public ConsultaResponse toResponse(Consulta consulta) {
        String status = consulta.getStatus() == null ? "AGENDADA" : consulta.getStatus().name();
        return new ConsultaResponse(
                consulta.getId(),
                consulta.getPacienteUsername(),
                consulta.getMedicoUsername(),
                consulta.getEnfermeiroUsername(),
                consulta.getDataHora(),
                consulta.getObservacoes(),
                status
        );
    }

    public ConsultaGraphQlType toGraphQlType(ConsultaResponse response) {
        return new ConsultaGraphQlType(
                response.id(),
                response.pacienteUsername(),
                response.medicoUsername(),
                response.enfermeiroUsername(),
                response.dataHora(),
                response.observacoes(),
                response.status()
        );
    }
}
