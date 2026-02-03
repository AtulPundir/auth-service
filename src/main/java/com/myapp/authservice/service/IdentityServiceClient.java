package com.myapp.authservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Client for calling identity-service internal endpoints.
 * Used to onboard users after OTP verification so that identity-service
 * can link placeholder users to the auth userId and resolve pending invitations.
 */
@Component
public class IdentityServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceClient.class);

    private final RestClient restClient;

    public IdentityServiceClient(@Value("${app.identity-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Notify identity-service that a user has completed OTP verification.
     * This links any existing placeholder user to the auth userId and resolves
     * pending invitations. Fire-and-forget — failures are logged but don't block login.
     */
    public void onboardUser(String authUserId, String name, String phone, String email) {
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("authUserId", authUserId);
            body.put("name", name);
            if (phone != null) body.put("phone", phone);
            if (email != null) body.put("email", email);
            body.put("isVerified", true);

            restClient.post()
                    .uri("/internal/users/onboard")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("Successfully onboarded user in identity-service: authUserId={}", authUserId);
        } catch (Exception e) {
            // Log but don't fail the login — identity-service linkage is best-effort
            // The user can still log in; linkage will happen on next login attempt
            logger.warn("Failed to onboard user in identity-service: authUserId={}, error={}",
                    authUserId, e.getMessage());
        }
    }
}
