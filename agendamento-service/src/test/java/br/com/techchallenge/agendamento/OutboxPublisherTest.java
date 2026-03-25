package br.com.techchallenge.agendamento;

import br.com.techchallenge.agendamento.config.OutboxPublisherProperties;
import br.com.techchallenge.agendamento.entity.OutboxEvent;
import br.com.techchallenge.agendamento.entity.OutboxStatus;
import br.com.techchallenge.agendamento.repository.AuditLogRepository;
import br.com.techchallenge.agendamento.repository.OutboxEventRepository;
import br.com.techchallenge.agendamento.service.AuditService;
import br.com.techchallenge.agendamento.service.OutboxPublisher;
import br.com.techchallenge.contract.messaging.ConsultaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpTemplate;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxPublisherTest {

    @Test
    void publicaEventoPendenteComSucesso() throws Exception {
        OutboxEvent outboxEvent = criarOutboxEvent();
        AtomicReference<ConsultaEvent> publishedEvent = new AtomicReference<>();
        OutboxPublisher outboxPublisher = criarPublisher(
                repoReturning(outboxEvent),
                amqpTemplateCapturing(publishedEvent)
        );

        outboxPublisher.publicarPendentesManualmente();

        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publishedEvent.get()).isNotNull();
        assertThat(publishedEvent.get().consultaId()).isEqualTo(1L);
    }

    @Test
    void marcaEventoComoFalhoQuandoBrokerQuebra() throws Exception {
        OutboxEvent outboxEvent = criarOutboxEvent();
        OutboxPublisher outboxPublisher = criarPublisher(
                repoReturning(outboxEvent),
                amqpTemplateThrowing()
        );

        outboxPublisher.publicarPendentesManualmente();

        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outboxEvent.getAttempts()).isEqualTo(1);
        assertThat(outboxEvent.getLastError()).contains("broker down");
    }

    private OutboxPublisher criarPublisher(OutboxEventRepository repository, AmqpTemplate amqpTemplate) {
        OutboxPublisherProperties properties = new OutboxPublisherProperties();
        properties.setBatchSize(10);
        properties.setLockTimeout(java.time.Duration.ofSeconds(30));
        AuditService auditService = new AuditService(noOpAuditRepository());
        return new OutboxPublisher(
                repository,
                amqpTemplate,
                new ObjectMapper().findAndRegisterModules(),
                properties,
                auditService
        );
    }

    private OutboxEvent criarOutboxEvent() throws Exception {
        ConsultaEvent event = new ConsultaEvent(
                "1.0",
                UUID.randomUUID().toString(),
                1L,
                "paciente1",
                "medico1",
                "enfermeiro1",
                LocalDateTime.of(2030, 1, 10, 14, 0),
                "Observacao",
                "AGENDADA",
                "CRIADA",
                Instant.now()
        );

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateId("1");
        outboxEvent.setAggregateType("CONSULTA");
        outboxEvent.setEventType("CRIADA");
        outboxEvent.setExchangeName("consulta.exchange");
        outboxEvent.setRoutingKey("consulta.criada.editada");
        outboxEvent.setPayload(new ObjectMapper().findAndRegisterModules().writeValueAsString(event));
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxEvent.setCreatedAt(Instant.now());
        outboxEvent.setNextAttemptAt(Instant.now());
        return outboxEvent;
    }

    private OutboxEventRepository repoReturning(OutboxEvent outboxEvent) {
        return (OutboxEventRepository) Proxy.newProxyInstance(
                OutboxEventRepository.class.getClassLoader(),
                new Class[]{OutboxEventRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findClaimableForUpdate" -> List.of(outboxEvent);
                    case "markPublished" -> {
                        outboxEvent.setStatus(OutboxStatus.PUBLISHED);
                        outboxEvent.setPublishedAt((Instant) args[3]);
                        outboxEvent.setLastError(null);
                        outboxEvent.setLockedAt(null);
                        outboxEvent.setLockedBy(null);
                        yield 1;
                    }
                    case "markFailed" -> {
                        outboxEvent.setStatus(OutboxStatus.FAILED);
                        outboxEvent.setAttempts(outboxEvent.getAttempts() + 1);
                        outboxEvent.setNextAttemptAt((Instant) args[3]);
                        outboxEvent.setLastError((String) args[4]);
                        outboxEvent.setLockedAt(null);
                        outboxEvent.setLockedBy(null);
                        yield 1;
                    }
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private AuditLogRepository noOpAuditRepository() {
        return (AuditLogRepository) Proxy.newProxyInstance(
                AuditLogRepository.class.getClassLoader(),
                new Class[]{AuditLogRepository.class},
                (proxy, method, args) -> defaultValue(method.getReturnType())
        );
    }

    private AmqpTemplate amqpTemplateCapturing(AtomicReference<ConsultaEvent> publishedEvent) {
        return (AmqpTemplate) Proxy.newProxyInstance(
                AmqpTemplate.class.getClassLoader(),
                new Class[]{AmqpTemplate.class},
                (proxy, method, args) -> {
                    if ("convertAndSend".equals(method.getName())) {
                        publishedEvent.set((ConsultaEvent) args[2]);
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private AmqpTemplate amqpTemplateThrowing() {
        return (AmqpTemplate) Proxy.newProxyInstance(
                AmqpTemplate.class.getClassLoader(),
                new Class[]{AmqpTemplate.class},
                (proxy, method, args) -> {
                    if ("convertAndSend".equals(method.getName())) {
                        throw new RuntimeException("broker down");
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }
}
