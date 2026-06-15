package com.insurance.backend.audit.repository;

import com.insurance.backend.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>
{
    List<AuditLog> findByUserEmailOrderByCreatedAtDesc(String email);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}