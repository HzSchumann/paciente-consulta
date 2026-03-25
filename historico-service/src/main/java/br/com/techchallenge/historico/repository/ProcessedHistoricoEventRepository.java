package br.com.techchallenge.historico.repository;

import br.com.techchallenge.historico.entity.ProcessedHistoricoEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedHistoricoEventRepository extends JpaRepository<ProcessedHistoricoEvent, String> {
}
