package br.com.techchallenge.agendamento.dto;

import java.time.LocalDateTime;

public record ConsultaEvent(
        Long consultaId,
        String pacienteUsername,
        LocalDateTime dataHora,
        String acao
) {
}
