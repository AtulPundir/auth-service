package com.myapp.authservice.repository;

import com.myapp.authservice.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AuditLog repository - matches Node.js Prisma client query methods
 * Simple save operations, no complex queries needed
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    // Matches: prisma.auditLog.create({ data: { ... } })
    // Inherited save() method handles this
}
