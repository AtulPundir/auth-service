package com.myapp.authservice.dto.response;

import com.myapp.authservice.domain.Role;
import com.myapp.authservice.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * User Response DTO (for /me endpoint)
 * Matches Node.js getUserById return type exactly
 *
 * TypeScript interface (from auth.service.ts):
 * {
 *   id: string;
 *   phone: string;
 *   name: string;
 *   role: string;
 *   status: string;
 *   createdAt: Date;
 *   updatedAt: Date;
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String phone;
    private String name;
    private String email;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
