package com.myapp.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Signup Request DTO
 * Matches Node.js SignupInput type exactly
 *
 * TypeScript interface:
 * interface SignupInput {
 *   phone: string;
 *   name: string;
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format (e.g., +919876543210)")
    private String phone;

    @NotBlank(message = "Name is required")
    private String name;
}
