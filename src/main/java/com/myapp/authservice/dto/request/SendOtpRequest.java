package com.myapp.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Send OTP Request DTO.
 * Supports either phone or email as the primary identifier.
 * At least one must be provided.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {

    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Phone number must be in E.164 format (e.g., +919876543210)")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    /**
     * Check if request has valid phone.
     */
    public boolean hasPhone() {
        return phone != null && !phone.isBlank();
    }

    /**
     * Check if request has valid email.
     */
    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }

    /**
     * Check if at least one identifier is provided.
     */
    public boolean hasAnyIdentifier() {
        return hasPhone() || hasEmail();
    }

    /**
     * Get the primary identifier (phone preferred over email).
     */
    public String getPrimaryIdentifier() {
        return hasPhone() ? phone : email;
    }
}
