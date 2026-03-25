package br.com.techchallenge.agendamento.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CriarConsultaRequest(
        @NotBlank String pacienteUsername,
        @NotBlank String medicoUsername,
        String enfermeiroUsername,
        @NotNull @Future LocalDateTime dataHora,
        String observacoes
) {
}
