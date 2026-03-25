package br.com.techchallenge.agendamento.dto;

import jakarta.validation.constraints.Future;

import java.time.LocalDateTime;

public record AtualizarConsultaInput(
        @Future LocalDateTime dataHora,
        String observacoes,
        String medicoUsername,
        String enfermeiroUsername
) {
    public AtualizarConsultaRequest toRequest() {
        return new AtualizarConsultaRequest(dataHora, observacoes, medicoUsername, enfermeiroUsername);
    }
}
