package br.com.techchallenge.agendamento.repository;

import br.com.techchallenge.agendamento.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
