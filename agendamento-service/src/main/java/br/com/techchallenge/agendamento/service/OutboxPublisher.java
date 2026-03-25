package br.com.techchallenge.agendamento.service;

import br.com.techchallenge.agendamento.config.OutboxPublisherProperties;
import br.com.techchallenge.agendamento.entity.OutboxEvent;
import br.com.techchallenge.agendamento.entity.OutboxStatus;
import br.com.techchallenge.agendamento.repository.OutboxEventRepository;
import br.com.techchallenge.contract.messaging.ConsultaEvent;
import org.springframework.amqp.core.AmqpTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final AmqpTemplate amqpTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxPublisherProperties properties;
    private final AuditService auditService;
    private final String publisherId = "publisher-" + UUID.randomUUID();

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            AmqpTemplate amqpTemplate,
            ObjectMapper objectMapper,
            OutboxPublisherProperties properties,
            AuditService auditService
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.amqpTemplate = amqpTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publisher.fixed-delay:1000}")
    @Transactional
    public void publicarPendentes() {
        if (!properties.isEnabled()) {
            return;
        }
        publicarPendentesManualmente();
    }

    @Transactional
    public void publicarPendentesManualmente() {
        Instant now = Instant.now();
        List<OutboxEvent> pendingEvents = outboxEventRepository.findClaimableForUpdate(
                Set.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                now,
                now.minus(properties.getLockTimeout()),
                PageRequest.of(0, properties.getBatchSize())
        );

        for (OutboxEvent outboxEvent : pendingEvents) {
            outboxEvent.setLockedAt(now);
            outboxEvent.setLockedBy(publisherId);
            long claimToken = outboxEvent.getClaimToken() == null ? 1L : outboxEvent.getClaimToken() + 1L;
            outboxEvent.setClaimToken(claimToken);
            try {
                ConsultaEvent event = objectMapper.readValue(outboxEvent.getPayload(), ConsultaEvent.class);
                amqpTemplate.convertAndSend(outboxEvent.getExchangeName(), outboxEvent.getRoutingKey(), event);
                int updatedRows = outboxEventRepository.markPublished(
                        outboxEvent.getId(),
                        publisherId,
                        claimToken,
                        Instant.now()
                );
                if (updatedRows == 0) {
                    throw new IllegalStateException("Claim do outbox perdido durante a publicacao");
                }
                auditService.registrar("system", "OUTBOX_PUBLICADO", "Evento " + outboxEvent.getId() + " publicado", true);
            } catch (Exception ex) {
                outboxEventRepository.markFailed(
                        outboxEvent.getId(),
                        publisherId,
                        claimToken,
                        Instant.now().plus(properties.getRetryBackoff()),
                        ex.getMessage()
                );
                auditService.registrar("system", "OUTBOX_FALHOU", "Evento " + outboxEvent.getId() + " falhou: " + ex.getMessage(), false);
            }
        }
    }

    public ConsultaEvent desserializar(OutboxEvent outboxEvent) {
        try {
            return objectMapper.readValue(outboxEvent.getPayload(), ConsultaEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Payload inválido no outbox", ex);
        }
    }
}
