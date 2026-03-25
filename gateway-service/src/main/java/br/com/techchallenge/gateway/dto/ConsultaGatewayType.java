package br.com.techchallenge.gateway.dto;

import java.time.LocalDateTime;

public record ConsultaGatewayType(
        Long id,
        String pacienteUsername,
        String medicoUsername,
        String enfermeiroUsername,
        LocalDateTime dataHora,
        String observacoes,
        String status,
        LocalDateTime ultimaAtualizacaoEm
) {
}
