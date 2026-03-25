package br.com.techchallenge.notificacao.service;

import br.com.techchallenge.contract.messaging.ConsultaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void enviar(ConsultaEvent event) {
        LOGGER.info("Lembrete enviado para paciente={} da consultaId={} em {} (acao={})",
                event.pacienteUsername(), event.consultaId(), event.dataHora(), event.acao());
    }
}
