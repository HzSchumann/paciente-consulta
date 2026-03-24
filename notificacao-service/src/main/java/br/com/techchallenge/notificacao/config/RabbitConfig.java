package br.com.techchallenge.notificacao.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_CONSULTA = "consulta.exchange";
    public static final String QUEUE_NOTIFICACAO = "consulta.notificacao.queue";
    public static final String ROUTING_KEY_CONSULTA = "consulta.criada.editada";

    @Bean
    DirectExchange consultaExchange() {
        return new DirectExchange(EXCHANGE_CONSULTA);
    }

    @Bean
    Queue notificacaoQueue() {
        return QueueBuilder.durable(QUEUE_NOTIFICACAO).build();
    }

    @Bean
    Binding notificacaoBinding(Queue notificacaoQueue, DirectExchange consultaExchange) {
        return BindingBuilder.bind(notificacaoQueue).to(consultaExchange).with(ROUTING_KEY_CONSULTA);
    }
}
