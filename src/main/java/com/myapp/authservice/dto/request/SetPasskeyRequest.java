package com.myapp.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Set Passkey Request DTO
 * Matches Node.js SetPasskeyInput type exactly
 *
 * TypeScript interface:
 * interface SetPasskeyInput {
 *   passkey: string;
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetPasskeyRequest {

    @NotBlank(message = "Passkey is required")
    @Size(min = 4, max = 20, message = "Passkey must be between 4 and 20 characters")
    private String passkey;
}
