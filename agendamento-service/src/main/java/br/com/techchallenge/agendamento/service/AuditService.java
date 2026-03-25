package br.com.techchallenge.agendamento.service;

import br.com.techchallenge.agendamento.entity.AuditLog;
import br.com.techchallenge.agendamento.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void registrar(String username, String action, String details, boolean success) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setSuccess(success);
        auditLog.setCreatedAt(Instant.now());
        auditLogRepository.save(auditLog);
    }
}
