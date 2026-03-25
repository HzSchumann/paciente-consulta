package br.com.techchallenge.notificacao.service;

import br.com.techchallenge.contract.messaging.ConsultaEvent;
import br.com.techchallenge.contract.messaging.ConsultaMessagingTopology;
import br.com.techchallenge.notificacao.entity.ProcessedEvent;
import br.com.techchallenge.notificacao.repository.ProcessedEventRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacaoListener {

    private final NotificationSender notificationSender;
    private final ProcessedEventRepository processedEventRepository;

    public NotificacaoListener(
            NotificationSender notificationSender,
            ProcessedEventRepository processedEventRepository
    ) {
        this.notificationSender = notificationSender;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @RabbitListener(queues = ConsultaMessagingTopology.QUEUE_NOTIFICACAO, containerFactory = "rabbitListenerContainerFactory")
    public void consumirEvento(ConsultaEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }
        notificationSender.enviar(event);
        processedEventRepository.save(new ProcessedEvent(event.eventId()));
    }
}
