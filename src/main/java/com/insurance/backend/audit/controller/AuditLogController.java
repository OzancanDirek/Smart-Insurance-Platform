package com.insurance.backend.audit.controller;

import com.insurance.backend.audit.entity.AuditLog;
import com.insurance.backend.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController
{
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllLogs(Authentication authentication)
    {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ADMIN"))
        {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<List<AuditLog>> getLogsByUser(@PathVariable String email, Authentication authentication)
    {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ADMIN"))
        {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getLogsByUser(email));
    }

    @GetMapping("/entity/{type}/{id}")
    public ResponseEntity<List<AuditLog>> getLogsByEntity(@PathVariable String type, @PathVariable Long id, Authentication authentication)
    {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ADMIN"))
        {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getLogsByEntity(type, id));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<AuditLog>> getAllLogsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication)
    {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        if (!role.equals("ROLE_ADMIN"))
        {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getAllLogsPaged(page, size));
    }
}