package br.com.techchallenge.notificacao.service;

import br.com.techchallenge.notificacao.config.RabbitConfig;
import br.com.techchallenge.notificacao.messaging.ConsultaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificacaoListener.class);

    @RabbitListener(queues = RabbitConfig.QUEUE_NOTIFICACAO)
    public void consumirEvento(ConsultaEvent event) {
        LOGGER.info("Lembrete enviado para paciente={} da consultaId={} em {} (acao={})",
                event.pacienteUsername(), event.consultaId(), event.dataHora(), event.acao());
    }
}
