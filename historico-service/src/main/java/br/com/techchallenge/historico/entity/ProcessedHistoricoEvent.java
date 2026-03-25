package br.com.techchallenge.historico.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processed_historico_events")
public class ProcessedHistoricoEvent {

    @Id
    @Column(nullable = false, updatable = false)
    private String eventId;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedHistoricoEvent() {
    }

    public ProcessedHistoricoEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
