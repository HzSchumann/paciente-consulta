package br.com.techchallenge.agendamento.dto;

import java.time.LocalDateTime;

public record ConsultaGraphQlType(
        Long id,
        String pacienteUsername,
        String medicoUsername,
        String enfermeiroUsername,
        LocalDateTime dataHora,
        String observacoes,
        String status
) {
}
