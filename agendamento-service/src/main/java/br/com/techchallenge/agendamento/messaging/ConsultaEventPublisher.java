package br.com.techchallenge.agendamento.messaging;

import br.com.techchallenge.agendamento.config.RabbitConfig;
import br.com.techchallenge.agendamento.dto.ConsultaEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConsultaEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ConsultaEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(ConsultaEvent event) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_CONSULTA, RabbitConfig.ROUTING_KEY_CONSULTA, event);
    }
}
