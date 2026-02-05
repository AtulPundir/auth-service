package com.myapp.authservice.service;

import com.myapp.authservice.domain.Role;
import com.myapp.authservice.domain.User;
import com.myapp.authservice.domain.UserStatus;
import com.myapp.authservice.dto.request.ResolveOrCreateUserRequest;
import com.myapp.authservice.dto.response.ResolveOrCreateUserResponse;
import com.myapp.authservice.repository.UserRepository;
import com.myapp.authservice.util.CuidGenerator;
import com.myapp.authservice.util.OtpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for internal user operations called by identity-service.
 * Ensures auth-service is the single source of truth for user IDs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InternalUserService {

    private final UserRepository userRepository;
    private final OtpUtils otpUtils;

    /**
     * Resolve an existing user or create a placeholder user.
     * This ensures auth-service generates the user ID first.
     *
     * @param request The resolve request containing phone or email
     * @return Response with user ID that identity-service must use
     */
    @Transactional
    public ResolveOrCreateUserResponse resolveOrCreateUser(ResolveOrCreateUserRequest request) {
        String normalizedKey = normalizeIdentityKey(request.getIdentityKey(), request.getIdentityType());

        // Try to find existing user
        User existingUser = findUserByIdentity(normalizedKey, request.getIdentityType());

        if (existingUser != null) {
            log.info("Found existing user: id={}, phone={}, isVerified={}",
                    existingUser.getId(), maskPhone(existingUser.getPhone()), existingUser.isVerified());

            return ResolveOrCreateUserResponse.builder()
                    .userId(existingUser.getId())
                    .isVerified(existingUser.isVerified())
                    .isNew(false)
                    .phone(existingUser.getPhone())
                    .email(existingUser.getEmail())
                    .name(existingUser.getName())
                    .build();
        }

        // Create placeholder user - handle race condition
        try {
            User placeholder = createPlaceholderUser(normalizedKey, request);
            log.info("Created placeholder user: id={}, phone={}", placeholder.getId(), maskPhone(placeholder.getPhone()));

            return ResolveOrCreateUserResponse.builder()
                    .userId(placeholder.getId())
                    .isVerified(false)
                    .isNew(true)
                    .phone(placeholder.getPhone())
                    .email(placeholder.getEmail())
                    .name(placeholder.getName())
                    .build();
        } catch (DataIntegrityViolationException e) {
            // Race condition - another request created the user, retry lookup
            log.info("Concurrent creation detected, retrying lookup for: {}", maskPhone(normalizedKey));
            User raceWinner = findUserByIdentity(normalizedKey, request.getIdentityType());
            if (raceWinner == null) {
                throw new IllegalStateException("User not found after constraint violation");
            }

            return ResolveOrCreateUserResponse.builder()
                    .userId(raceWinner.getId())
                    .isVerified(raceWinner.isVerified())
                    .isNew(false)
                    .phone(raceWinner.getPhone())
                    .email(raceWinner.getEmail())
                    .name(raceWinner.getName())
                    .build();
        }
    }

    private User findUserByIdentity(String normalizedKey, ResolveOrCreateUserRequest.IdentityType type) {
        if (type == ResolveOrCreateUserRequest.IdentityType.EMAIL) {
            return userRepository.findByEmailIgnoreCase(normalizedKey).orElse(null);
        }

        // For phone, try exact match first
        User user = userRepository.findByPhone(normalizedKey).orElse(null);
        if (user != null) {
            return user;
        }

        // Try matching last 10 digits (handles country code variations)
        String last10 = extractLast10Digits(normalizedKey);
        if (last10 != null) {
            return userRepository.findByPhoneSuffix(last10).orElse(null);
        }

        return null;
    }

    private User createPlaceholderUser(String normalizedKey, ResolveOrCreateUserRequest request) {
        String userId = CuidGenerator.generate();
        String name = request.getName() != null ? request.getName() : "Guest";

        User.UserBuilder builder = User.builder()
                .id(userId)
                .name(name)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isVerified(false); // Placeholder - not verified until OTP login

        if (request.getIdentityType() == ResolveOrCreateUserRequest.IdentityType.EMAIL) {
            builder.email(normalizedKey);
            // Phone is required, use a placeholder
            builder.phone(generatePlaceholderPhone(userId));
        } else {
            builder.phone(normalizedKey);
        }

        return userRepository.save(builder.build());
    }

    private String normalizeIdentityKey(String key, ResolveOrCreateUserRequest.IdentityType type) {
        if (type == ResolveOrCreateUserRequest.IdentityType.EMAIL) {
            return key.toLowerCase().trim();
        }

        // Phone: ensure E.164 format
        String cleaned = key.replaceAll("[^\\d+]", "");

        // Phone must include country code (start with +)
        if (!cleaned.startsWith("+")) {
            throw new IllegalArgumentException(
                "Phone number must include country code in E.164 format (e.g., +919876543210)");
        }

        // Validate minimum length: + plus at least 9 digits
        if (cleaned.length() < 10) {
            throw new IllegalArgumentException(
                "Phone number is too short. Must be in E.164 format with country code");
        }

        return cleaned;
    }

    private String extractLast10Digits(String phone) {
        String digits = phone.replaceAll("[^\\d]", "");
        if (digits.length() >= 10) {
            return digits.substring(digits.length() - 10);
        }
        return null;
    }

    private String generatePlaceholderPhone(String userId) {
        // Generate a unique placeholder phone for email-only users
        // This satisfies the NOT NULL constraint while being clearly identifiable
        return "+0000" + userId.substring(0, Math.min(10, userId.length()));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "****";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
