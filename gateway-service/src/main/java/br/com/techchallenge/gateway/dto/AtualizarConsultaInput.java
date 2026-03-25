package br.com.techchallenge.gateway.dto;

import java.time.LocalDateTime;

public record AtualizarConsultaInput(
        LocalDateTime dataHora,
        String observacoes,
        String medicoUsername,
        String enfermeiroUsername
) {
}
