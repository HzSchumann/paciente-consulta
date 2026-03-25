package br.com.techchallenge.gateway.dto;

import java.time.LocalDateTime;

public record CriarConsultaInput(
        String pacienteUsername,
        String medicoUsername,
        String enfermeiroUsername,
        LocalDateTime dataHora,
        String observacoes
) {
}
