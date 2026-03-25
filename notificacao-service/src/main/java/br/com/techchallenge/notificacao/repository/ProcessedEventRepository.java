package br.com.techchallenge.notificacao.repository;

import br.com.techchallenge.notificacao.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
