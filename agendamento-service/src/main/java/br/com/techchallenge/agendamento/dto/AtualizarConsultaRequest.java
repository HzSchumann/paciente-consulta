package br.com.techchallenge.agendamento.dto;

import jakarta.validation.constraints.Future;

import java.time.LocalDateTime;

public record AtualizarConsultaRequest(
        @Future LocalDateTime dataHora,
        String observacoes,
        String medicoUsername,
        String enfermeiroUsername
) {
}
