package com.myapp.authservice.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_phone", columnList = "phone"),
    @Index(name = "idx_user_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"refreshTokens", "otpCodes", "auditLogs"})
@EqualsAndHashCode(of = "id")
public class User {

    @Id
    @Column(length = 30)
    private String id; // CUID generated in service layer

    @Column(unique = true, nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private String name;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Role role = Role.USER;

    @Column(length = 60) // BCrypt hash is 60 chars
    private String passkeyHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Relationships - use lazy loading for performance
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OtpCode> otpCodes = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuditLog> auditLogs = new ArrayList<>();
}
