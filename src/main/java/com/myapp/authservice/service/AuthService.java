package com.myapp.authservice.service;

import com.myapp.authservice.domain.*;
import com.myapp.authservice.dto.response.OtpSentResponse;
import com.myapp.authservice.dto.response.AuthResponse;
import com.myapp.authservice.dto.response.TokenResponse;
import com.myapp.authservice.dto.response.UserResponse;
import com.myapp.authservice.exception.BadRequestException;
import com.myapp.authservice.exception.NotFoundException;
import com.myapp.authservice.exception.UnauthorizedException;
import com.myapp.authservice.repository.AuditLogRepository;
import com.myapp.authservice.repository.OtpCodeRepository;
import com.myapp.authservice.repository.RefreshTokenRepository;
import com.myapp.authservice.repository.UserRepository;
import com.myapp.authservice.util.CuidGenerator;
import com.myapp.authservice.util.JwtUtils;
import com.myapp.authservice.util.OtpUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final AuditLogRepository auditLogRepository;
    private final JwtUtils jwtUtils;
    private final OtpUtils otpUtils;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitingService rateLimitingService;
    private final IdentityServiceClient identityServiceClient;

    @Value("${app.jwt.refresh-token-expiry-days}")
    private long refreshTokenExpiryDays;

    /**
     * Send OTP to phone number
     */
    @Transactional
    public OtpSentResponse sendOtp(String phone, String ipAddress, String userAgent) {
        String formattedPhone = otpUtils.formatPhone(phone);

        // Check rate limit (per phone number)
        rateLimitingService.checkOtpSendLimit(formattedPhone);

        String otp = otpUtils.generateOtp();
        //The OTP log is for local purpose, THIS SHOULD BE REMOVED
        log.info("OTP Generated: {}", otp);
        LocalDateTime expiresAt = otpUtils.getExpiryDate();

        // Save OTP to database
        OtpCode otpCode = OtpCode.builder()
                .id(CuidGenerator.generate())
                .phone(formattedPhone)
                .code(otp)
                .expiresAt(expiresAt)
                .build();
        otpCodeRepository.save(otpCode);

        // Audit log (never log the OTP code itself)
        createAuditLog(null, AuditAction.OTP_SENT, formattedPhone, ipAddress, userAgent,
                Map.of("expiresAt", expiresAt.toString()), true);

        // Log only masked phone, never the OTP code (security best practice)
        log.info("OTP sent to phone: {}", maskPhone(formattedPhone));

        return OtpSentResponse.builder()
                .message("OTP sent successfully")
                .expiresIn((int) otpUtils.getExpirySeconds())
                .build();
    }

    /**
     * Verify OTP and login/signup user
     */
    @Transactional
    public AuthResponse verifyOtpAndLogin(String phone, String otp, String name,
                                           String ipAddress, String userAgent) {
        String formattedPhone = otpUtils.formatPhone(phone);

        // Check rate limit (per phone number) before verification
        rateLimitingService.checkOtpVerifyLimit(formattedPhone);

        // Find valid OTP
        Optional<OtpCode> otpCodeOpt = otpCodeRepository.findValidOtp(
                formattedPhone, otp, LocalDateTime.now());

        if (otpCodeOpt.isEmpty()) {
            createAuditLog(null, AuditAction.OTP_FAILED, formattedPhone, ipAddress, userAgent,
                    Map.of("reason", "Invalid or expired OTP"), false);
            throw new UnauthorizedException("Invalid or expired OTP");
        }

        OtpCode otpCode = otpCodeOpt.get();

        // Mark OTP as used
        otpCode.setUsed(true);
        otpCodeRepository.save(otpCode);

        // Mark all other OTPs for this phone as used
        otpCodeRepository.markAllAsUsedForPhone(formattedPhone);

        // Find or create user
        Optional<User> existingUser = userRepository.findByPhone(formattedPhone);
        User user;
        boolean isNewUser = existingUser.isEmpty();

        if (isNewUser) {
            // Use provided name or default to "Guest"
            String userName = (name != null && !name.isBlank()) ? name : "Guest";
            user = User.builder()
                    .id(CuidGenerator.generate())
                    .phone(formattedPhone)
                    .name(userName)
                    .role(Role.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);

            createAuditLog(user, AuditAction.USER_SIGNUP, formattedPhone, ipAddress, userAgent,
                    Map.of("name", userName), true);
            log.info("New user created: {}", user.getId());
        } else {
            user = existingUser.get();
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new UnauthorizedException("Account is not active");
            }
        }

        // Generate tokens
        AuthResponse authResponse = generateAuthResponse(user);

        // Save refresh token
        saveRefreshToken(user, authResponse.getRefreshToken());

        // Audit log for successful OTP verification
        createAuditLog(user, AuditAction.OTP_VERIFIED, formattedPhone, ipAddress, userAgent,
                Map.of("isNewUser", isNewUser), true);

        // Audit log for login
        createAuditLog(user, AuditAction.USER_LOGIN_OTP, formattedPhone, ipAddress, userAgent,
                null, true);

        // Reset rate limit on successful verification
        rateLimitingService.resetOtpVerifyLimit(formattedPhone);

        // Notify identity-service so it can link placeholder users and resolve invitations
        identityServiceClient.onboardUser(user.getId(), user.getName(), formattedPhone, user.getEmail());

        return authResponse;
    }

    /**
     * Login with passkey (PIN)
     */
    @Transactional
    public AuthResponse loginWithPasskey(String phone, String passkey,
                                          String ipAddress, String userAgent) {
        String formattedPhone = otpUtils.formatPhone(phone);

        // Check rate limit (per phone number) before passkey verification
        rateLimitingService.checkPasskeyLoginLimit(formattedPhone);

        User user = userRepository.findByPhone(formattedPhone)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }

        if (user.getPasskeyHash() == null) {
            throw new BadRequestException("Passkey not set. Please use OTP login first.");
        }

        if (!passwordEncoder.matches(passkey, user.getPasskeyHash())) {
            createAuditLog(user, AuditAction.PASSKEY_LOGIN_FAILED, formattedPhone,
                    ipAddress, userAgent, null, false);
            throw new UnauthorizedException("Invalid passkey");
        }

        // Generate tokens
        AuthResponse authResponse = generateAuthResponse(user);

        // Save refresh token
        saveRefreshToken(user, authResponse.getRefreshToken());

        // Audit log
        createAuditLog(user, AuditAction.USER_LOGIN_PASSKEY, formattedPhone,
                ipAddress, userAgent, null, true);

        // Reset rate limit on successful login
        rateLimitingService.resetPasskeyLoginLimit(formattedPhone);

        // Notify identity-service so it can link placeholder users and resolve invitations
        identityServiceClient.onboardUser(user.getId(), user.getName(), formattedPhone, user.getEmail());

        return authResponse;
    }

    /**
     * Set or update passkey for authenticated user
     */
    @Transactional
    public void setPasskey(String userId, String passkey, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String hashedPasskey = passwordEncoder.encode(passkey);
        user.setPasskeyHash(hashedPasskey);
        userRepository.save(user);

        // Audit log
        createAuditLog(user, AuditAction.PASSKEY_SET, user.getPhone(),
                ipAddress, userAgent, null, true);

        log.info("Passkey set for user: {}", userId);
    }

    /**
     * Refresh access token using refresh token
     */
    @Transactional
    public TokenResponse refreshToken(String refreshToken, String ipAddress, String userAgent) {
        // Verify refresh token JWT
        Claims claims;
        try {
            claims = jwtUtils.verifyRefreshToken(refreshToken);
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String userId = claims.get("userId", String.class);

        // Find refresh token in database
        RefreshToken storedToken = refreshTokenRepository.findByTokenWithUser(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found or revoked"));

        User user = storedToken.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }

        // Delete ALL refresh tokens for this user to prevent race-condition duplicates
        // when multiple API clients attempt concurrent refreshes
        refreshTokenRepository.deleteAllByUserId(user.getId());
        refreshTokenRepository.flush();

        // Generate new tokens
        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getPhone(), user.getRole());
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId());

        // Save new refresh token
        saveRefreshToken(user, newRefreshToken);

        // Audit log
        createAuditLog(user, AuditAction.TOKEN_REFRESHED, user.getPhone(),
                ipAddress, userAgent, null, true);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * Logout user - invalidate refresh token(s)
     */
    @Transactional
    public void logout(String userId, String refreshToken, String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (refreshToken != null && !refreshToken.isBlank()) {
            // Logout specific device
            refreshTokenRepository.deleteByUserIdAndToken(userId, refreshToken);
        } else {
            // Logout all devices
            refreshTokenRepository.deleteAllByUserId(userId);
        }

        // Audit log
        createAuditLog(user, AuditAction.USER_LOGOUT, user.getPhone(),
                ipAddress, userAgent,
                Map.of("allDevices", refreshToken == null || refreshToken.isBlank()), true);

        log.info("User logged out: {}", userId);
    }

    /**
     * Get current user details
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserResponse updateProfile(String userId, String name, String email,
                                       String ipAddress, String userAgent) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Update fields if provided
        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }
        if (email != null && !email.isBlank()) {
            user.setEmail(email.trim().toLowerCase());
        }

        userRepository.save(user);

        // Audit log
        createAuditLog(user, AuditAction.PROFILE_UPDATED, user.getPhone(),
                ipAddress, userAgent,
                Map.of("updatedFields", (name != null ? "name," : "") + (email != null ? "email" : "")),
                true);

        log.info("Profile updated for user: {}", userId);

        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Scheduled cleanup of expired tokens and OTPs
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();

        int deletedTokens = refreshTokenRepository.deleteExpiredTokens(now);
        int deletedOtps = otpCodeRepository.deleteExpiredAndUsedOtps(now);

        log.info("Cleanup completed: {} tokens, {} OTPs deleted", deletedTokens, deletedOtps);
    }

    // Helper methods

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getPhone(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .phone(user.getPhone())
                        .name(user.getName())
                        .role(user.getRole())
                        .build())
                .build();
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken refreshToken = RefreshToken.builder()
                .id(CuidGenerator.generate())
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private void createAuditLog(User user, AuditAction action, String phone,
                                 String ipAddress, String userAgent,
                                 Map<String, Object> metadata, boolean success) {
        AuditLog auditLog = AuditLog.builder()
                .id(CuidGenerator.generate())
                .user(user)
                .action(action)
                .phone(phone)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .metadata(metadata)
                .success(success)
                .build();
        auditLogRepository.save(auditLog);
    }

    /**
     * Mask phone number for logging (security best practice)
     * Example: +919876543210 -> +91****3210
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return "****";
        }
        // Keep country code and last 4 digits
        int len = phone.length();
        String prefix = phone.substring(0, Math.min(3, len));
        String suffix = phone.substring(len - 4);
        return prefix + "****" + suffix;
    }
}
