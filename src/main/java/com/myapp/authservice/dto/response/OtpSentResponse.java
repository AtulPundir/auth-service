package com.myapp.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OTP Sent Response DTO
 * Matches Node.js sendOtp return type exactly
 *
 * TypeScript interface:
 * interface { message: string; expiresIn: number }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSentResponse {

    private String message;
    private Integer expiresIn; // Seconds until OTP expires
}
