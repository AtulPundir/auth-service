package com.myapp.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request from identity-service to resolve or create a user by phone/email.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveOrCreateUserRequest {

    @NotBlank(message = "Identity key (phone or email) is required")
    private String identityKey;

    @NotNull(message = "Identity type is required")
    private IdentityType identityType;

    private String name;

    public enum IdentityType {
        PHONE,
        EMAIL
    }
}
