package com.myapp.authservice.dto.response;

import com.myapp.authservice.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication Response DTO
 * Matches Node.js AuthResponse type exactly
 *
 * TypeScript interface:
 * interface AuthResponse {
 *   accessToken: string;
 *   refreshToken: string;
 *   user: {
 *     id: string;
 *     phone: string;
 *     name: string;
 *     role: string;
 *   };
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String phone;
        private String name;
        private Role role;
    }
}
