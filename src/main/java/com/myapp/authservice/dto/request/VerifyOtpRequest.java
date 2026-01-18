package com.myapp.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Verify OTP Request DTO
 * Matches Node.js VerifyOtpInput type exactly
 *
 * TypeScript interface:
 * interface VerifyOtpInput {
 *   phone: string;
 *   otp: string;
 *   name?: string;
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format (e.g., +919876543210)")
    private String phone;

    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 10, message = "OTP must be between 4 and 10 characters")
    private String otp;

    private String name; // Required for new users
}
