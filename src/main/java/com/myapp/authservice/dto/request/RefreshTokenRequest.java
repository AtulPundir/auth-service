package com.myapp.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh Token Request DTO
 * Matches Node.js RefreshTokenInput type exactly
 *
 * TypeScript interface:
 * interface RefreshTokenInput {
 *   refreshToken: string;
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
