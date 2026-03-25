package br.com.techchallenge.historico.config;

import br.com.techchallenge.contract.messaging.ConsultaMessagingTopology;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
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
    Queue historicoQueue() {
        return QueueBuilder.durable(ConsultaMessagingTopology.QUEUE_HISTORICO)
                .deadLetterExchange(ConsultaMessagingTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ConsultaMessagingTopology.HISTORICO_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue historicoDeadLetterQueue() {
        return QueueBuilder.durable(ConsultaMessagingTopology.HISTORICO_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Declarables declarables(
            Queue historicoQueue,
            Queue historicoDeadLetterQueue,
            DirectExchange consultaExchange,
            DirectExchange deadLetterExchange
    ) {
        Binding historicoBinding = BindingBuilder.bind(historicoQueue)
                .to(consultaExchange)
                .with(ConsultaMessagingTopology.ROUTING_KEY_CONSULTA);
        Binding deadLetterBinding = BindingBuilder.bind(historicoDeadLetterQueue)
                .to(deadLetterExchange)
                .with(ConsultaMessagingTopology.HISTORICO_DEAD_LETTER_ROUTING_KEY);
        return new Declarables(
                consultaExchange,
                deadLetterExchange,
                historicoQueue,
                historicoDeadLetterQueue,
                historicoBinding,
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
                ConsultaMessagingTopology.HISTORICO_DEAD_LETTER_ROUTING_KEY
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
            Advice retryInterceptor,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setAdviceChain(retryInterceptor);
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(autoStartup);
        return factory;
    }
}
