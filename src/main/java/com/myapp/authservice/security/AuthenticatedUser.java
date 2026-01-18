package com.myapp.authservice.security;

import com.myapp.authservice.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents an authenticated user extracted from JWT token.
 * Used as the principal in Spring Security context.
 */
@Getter
@AllArgsConstructor
public class AuthenticatedUser {
    private final String userId;
    private final String phone;
    private final Role role;
}
