package com.insurance.backend.audit.service;

import com.insurance.backend.audit.entity.AuditLog;
import com.insurance.backend.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService
{
    private final AuditLogRepository auditLogRepository;

    public void log(String userEmail, String action, String entityType, Long entityId, String detail)
    {
        AuditLog log = AuditLog.builder()
                .userEmail(userEmail)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .detail(detail)
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLog> getAllLogs()
    {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<AuditLog> getLogsByUser(String email)
    {
        return auditLogRepository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    public List<AuditLog> getLogsByEntity(String entityType, Long entityId)
    {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }

    public Page<AuditLog> getAllLogsPaged(int page, int size)
    {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}