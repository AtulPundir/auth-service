package com.myapp.authservice.controller;

import com.myapp.authservice.dto.request.ResolveOrCreateUserRequest;
import com.myapp.authservice.dto.response.ApiResponse;
import com.myapp.authservice.dto.response.ResolveOrCreateUserResponse;
import com.myapp.authservice.service.InternalUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API endpoints for service-to-service communication.
 * Called by identity-service to resolve or create placeholder users.
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final InternalUserService internalUserService;

    /**
     * Resolve an existing user or create a placeholder user by phone/email.
     * Called by identity-service when creating trips/groups with contacts.
     *
     * Returns the auth-service user ID which identity-service must use as its user ID.
     */
    @PostMapping("/users/resolve-or-create")
    public ResponseEntity<ApiResponse<ResolveOrCreateUserResponse>> resolveOrCreateUser(
            @Valid @RequestBody ResolveOrCreateUserRequest request) {

        log.info("POST /internal/users/resolve-or-create - identityKey={}, identityType={}",
                maskIdentifier(request.getIdentityKey()), request.getIdentityType());

        ResolveOrCreateUserResponse response = internalUserService.resolveOrCreateUser(request);

        log.info("POST /internal/users/resolve-or-create - userId={}, isVerified={}, isNew={}",
                response.getUserId(), response.isVerified(), response.isNew());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) return "***";
        if (identifier.contains("@")) {
            int atIndex = identifier.indexOf('@');
            return identifier.substring(0, Math.min(2, atIndex)) + "***" + identifier.substring(atIndex);
        }
        return identifier.substring(0, 3) + "***" + identifier.substring(identifier.length() - 2);
    }
}
