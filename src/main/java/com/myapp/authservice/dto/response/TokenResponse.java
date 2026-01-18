package com.myapp.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token Response DTO (for refresh endpoint)
 * Matches Node.js AuthTokens type exactly
 *
 * TypeScript interface:
 * interface AuthTokens {
 *   accessToken: string;
 *   refreshToken: string;
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
}
