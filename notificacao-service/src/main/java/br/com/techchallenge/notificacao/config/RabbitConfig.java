package br.com.techchallenge.notificacao.config;

import br.com.techchallenge.contract.messaging.ConsultaMessagingTopology;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    DirectExchange consultaExchange() {
        return new DirectExchange(ConsultaMessagingTopology.EXCHANGE_CONSULTA);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(ConsultaMessagingTopology.DEAD_LETTER_EXCHANGE);
    }

    @Bean
    Queue notificacaoQueue() {
        return QueueBuilder.durable(ConsultaMessagingTopology.QUEUE_NOTIFICACAO)
                .deadLetterExchange(ConsultaMessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ConsultaMessagingTopology.NOTIFICACAO_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue notificacaoDeadLetterQueue() {
        return QueueBuilder.durable(ConsultaMessagingTopology.NOTIFICACAO_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Declarables declarables(
            Queue notificacaoQueue,
            Queue notificacaoDeadLetterQueue,
            DirectExchange consultaExchange,
            DirectExchange deadLetterExchange
    ) {
        Binding notificacaoBinding = BindingBuilder.bind(notificacaoQueue)
                .to(consultaExchange)
                .with(ConsultaMessagingTopology.ROUTING_KEY_CONSULTA);
        Binding deadLetterBinding = BindingBuilder.bind(notificacaoDeadLetterQueue)
                .to(deadLetterExchange)
                .with(ConsultaMessagingTopology.NOTIFICACAO_DEAD_LETTER_ROUTING_KEY);
        return new Declarables(
                consultaExchange,
                deadLetterExchange,
                notificacaoQueue,
                notificacaoDeadLetterQueue,
                notificacaoBinding,
                deadLetterBinding
        );
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(
                rabbitTemplate,
                ConsultaMessagingTopology.DEAD_LETTER_EXCHANGE,
                ConsultaMessagingTopology.NOTIFICACAO_DEAD_LETTER_ROUTING_KEY
        );
    }

    @Bean
    Advice retryInterceptor(MessageRecoverer messageRecoverer) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 5000)
                .recoverer(messageRecoverer)
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter,
            Advice retryInterceptor
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setAdviceChain(retryInterceptor);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
