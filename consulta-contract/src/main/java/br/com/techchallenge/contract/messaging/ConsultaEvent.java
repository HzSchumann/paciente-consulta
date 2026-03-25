package br.com.techchallenge.contract.messaging;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;

public record ConsultaEvent(
        String schemaVersion,
        String eventId,
        Long consultaId,
        String pacienteUsername,
        String medicoUsername,
        String enfermeiroUsername,
        LocalDateTime dataHora,
        String observacoes,
        String status,
        String acao,
        Instant occurredAt
) implements Serializable {
}
