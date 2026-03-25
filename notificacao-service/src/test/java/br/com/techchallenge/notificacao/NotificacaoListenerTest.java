package br.com.techchallenge.notificacao;

import br.com.techchallenge.contract.messaging.ConsultaEvent;
import br.com.techchallenge.notificacao.entity.ProcessedEvent;
import br.com.techchallenge.notificacao.repository.ProcessedEventRepository;
import br.com.techchallenge.notificacao.service.NotificacaoListener;
import br.com.techchallenge.notificacao.service.NotificationSender;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class NotificacaoListenerTest {

    @Test
    void consomeEventoEDelegadaEnvio() {
        AtomicReference<ConsultaEvent> received = new AtomicReference<>();
        NotificationSender sender = received::set;
        Set<String> processedEvents = new HashSet<>();
        ProcessedEventRepository processedEventRepository = inMemoryRepository(processedEvents);
        NotificacaoListener notificacaoListener = new NotificacaoListener(sender, processedEventRepository);

        ConsultaEvent event = new ConsultaEvent(
                "1.0",
                "event-1",
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

        notificacaoListener.consumirEvento(event);

        assertThat(received.get()).isEqualTo(event);
        assertThat(processedEvents).contains("event-1");
    }

    @Test
    void ignoraEventoDuplicado() {
        AtomicReference<ConsultaEvent> received = new AtomicReference<>();
        NotificationSender sender = received::set;
        Set<String> processedEvents = new HashSet<>();
        processedEvents.add("event-1");
        ProcessedEventRepository processedEventRepository = inMemoryRepository(processedEvents);
        NotificacaoListener notificacaoListener = new NotificacaoListener(sender, processedEventRepository);

        ConsultaEvent event = new ConsultaEvent(
                "1.0",
                "event-1",
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

        notificacaoListener.consumirEvento(event);

        assertThat(received.get()).isNull();
    }

    private ProcessedEventRepository inMemoryRepository(Set<String> processedEvents) {
        return (ProcessedEventRepository) Proxy.newProxyInstance(
                ProcessedEventRepository.class.getClassLoader(),
                new Class[]{ProcessedEventRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "existsById" -> processedEvents.contains((String) args[0]);
                    case "save" -> {
                        ProcessedEvent event = (ProcessedEvent) args[0];
                        processedEvents.add(event.getEventId());
                        yield event;
                    }
                    default -> null;
                }
        );
    }
}
