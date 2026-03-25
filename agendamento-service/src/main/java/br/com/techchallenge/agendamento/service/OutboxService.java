package br.com.techchallenge.agendamento.service;

import br.com.techchallenge.agendamento.entity.OutboxEvent;
import br.com.techchallenge.agendamento.entity.OutboxStatus;
import br.com.techchallenge.agendamento.repository.OutboxEventRepository;
import br.com.techchallenge.contract.messaging.ConsultaEvent;
import br.com.techchallenge.contract.messaging.ConsultaMessagingTopology;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void registrarConsultaEvento(ConsultaEvent event) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType("CONSULTA");
        outboxEvent.setAggregateId(String.valueOf(event.consultaId()));
        outboxEvent.setEventType(event.acao());
        outboxEvent.setExchangeName(ConsultaMessagingTopology.EXCHANGE_CONSULTA);
        outboxEvent.setRoutingKey(ConsultaMessagingTopology.ROUTING_KEY_CONSULTA);
        outboxEvent.setPayload(serializar(event));
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setAttempts(0);
        outboxEvent.setCreatedAt(Instant.now());
        outboxEvent.setNextAttemptAt(Instant.now());
        outboxEventRepository.save(outboxEvent);
    }

    private String serializar(ConsultaEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível serializar evento de consulta", ex);
        }
    }
}
