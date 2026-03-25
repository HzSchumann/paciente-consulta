package br.com.techchallenge.historico.dto;

import java.time.LocalDateTime;

public record ConsultaHistoricoGraphQlType(
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
