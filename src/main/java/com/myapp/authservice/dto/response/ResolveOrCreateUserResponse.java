package com.myapp.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing the resolved or created user ID.
 * identity-service MUST use this userId as both id and auth_user_id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveOrCreateUserResponse {

    /**
     * The auth-service user ID. Identity-service must use this as its user ID
     * to ensure IDs are consistent across services.
     */
    private String userId;

    /**
     * True if the user has completed OTP verification.
     * False for placeholder users who haven't logged in yet.
     */
    private boolean isVerified;

    /**
     * True if a new placeholder user was created.
     * False if an existing user was found.
     */
    private boolean isNew;

    /**
     * The user's phone number (normalized to E.164 format)
     */
    private String phone;

    /**
     * The user's email (if available)
     */
    private String email;

    /**
     * The user's display name
     */
    private String name;
}
