package br.com.techchallenge.notificacao.messaging;

import java.time.LocalDateTime;

public record ConsultaEvent(
        Long consultaId,
        String pacienteUsername,
        LocalDateTime dataHora,
        String acao
) {
}
