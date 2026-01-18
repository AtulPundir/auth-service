package com.myapp.authservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AuditLog entity - matches Node.js Prisma AuditLog model exactly
 *
 * Prisma schema:
 * model AuditLog {
 *   id          String      @id @default(cuid())
 *   userId      String?
 *   action      AuditAction
 *   phone       String?
 *   ipAddress   String?
 *   userAgent   String?
 *   metadata    Json?
 *   success     Boolean     @default(true)
 *   createdAt   DateTime    @default(now())
 *   user        User?       @relation(fields: [userId], references: [id], onDelete: SetNull)
 * }
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_created_at", columnList = "created_at"),
    @Index(name = "idx_audit_phone", columnList = "phone")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(of = "id")
public class AuditLog {

    @Id
    @Column(length = 30)
    private String id; // CUID generated in service layer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Column(length = 20)
    private String phone;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(nullable = false)
    @Builder.Default
    private Boolean success = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
