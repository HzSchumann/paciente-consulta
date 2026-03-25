package br.com.techchallenge.historico.service;

import br.com.techchallenge.contract.messaging.ConsultaEvent;
import br.com.techchallenge.contract.messaging.ConsultaMessagingTopology;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class HistoricoProjectionConsumer {

    private final HistoricoConsultaService historicoConsultaService;

    public HistoricoProjectionConsumer(HistoricoConsultaService historicoConsultaService) {
        this.historicoConsultaService = historicoConsultaService;
    }

    @RabbitListener(queues = ConsultaMessagingTopology.QUEUE_HISTORICO, containerFactory = "rabbitListenerContainerFactory")
    public void consumirEvento(ConsultaEvent event) {
        historicoConsultaService.projetar(event);
    }
}
