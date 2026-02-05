package com.myapp.authservice.controller;

import com.myapp.authservice.dto.request.*;
import com.myapp.authservice.dto.response.*;
import com.myapp.authservice.security.AuthenticatedUser;
import com.myapp.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Send OTP to phone number or email.
     * At least one of phone or email must be provided. Phone takes priority.
     * POST /auth/otp/send
     */
    @PostMapping("/otp/send")
    public ResponseEntity<ApiResponse<OtpSentResponse>> sendOtp(
            @Valid @RequestBody SendOtpRequest request,
            HttpServletRequest httpRequest) {

        // Validate at least one identifier is provided
        if (!request.hasAnyIdentifier()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Either phone or email is required"));
        }

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        OtpSentResponse response;

        // Phone takes priority if both provided
        if (request.hasPhone()) {
            log.info("POST /auth/otp/send - Request: phone={}", maskPhone(request.getPhone()));
            response = authService.sendOtp(request.getPhone(), ipAddress, userAgent);
        } else {
            log.info("POST /auth/otp/send - Request: email={}", maskEmail(request.getEmail()));
            response = authService.sendOtpToEmail(request.getEmail(), ipAddress, userAgent);
        }

        log.info("POST /auth/otp/send - Response: success=true, expiresIn={}", response.getExpiresIn());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Verify OTP and login/signup
     * POST /auth/otp/verify
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /auth/otp/verify - Request: phone={}, otp=****, name={}",
                maskPhone(request.getPhone()), request.getName());

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.verifyOtpAndLogin(
                request.getPhone(),
                request.getOtp(),
                request.getName(),
                ipAddress,
                userAgent);

        log.info("POST /auth/otp/verify - Response: success=true, userId={}", response.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

/*
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request) {

        SignupResponse result = authService.signup(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        result,
                        "OTP sent to your phone"
                )
        );
    }*/

    /**
     * Login with passkey (PIN)
     * POST /auth/passkey/login
     */
    @PostMapping("/passkey/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithPasskey(
            @Valid @RequestBody LoginPasskeyRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.loginWithPasskey(
                request.getPhone(),
                request.getPasskey(),
                ipAddress,
                userAgent);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Set or update passkey for authenticated user
     * POST /auth/passkey/set
     */
    @PostMapping("/passkey/set")
    public ResponseEntity<ApiResponse<MessageResponse>> setPasskey(
            @Valid @RequestBody SetPasskeyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        authService.setPasskey(user.getUserId(), request.getPasskey(), ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success(
                MessageResponse.of("Passkey set successfully")));
    }

    /**
     * Refresh access token
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        TokenResponse response = authService.refreshToken(
                request.getRefreshToken(), ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Logout user
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(
            @RequestBody(required = false) LogoutRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String refreshToken = request != null ? request.getRefreshToken() : null;

        authService.logout(user.getUserId(), refreshToken, ipAddress, userAgent);

        String message = (refreshToken == null || refreshToken.isBlank())
                ? "Logged out from all devices"
                : "Logged out successfully";

        return ResponseEntity.ok(ApiResponse.success(MessageResponse.of(message)));
    }

    /**
     * Get current user details
     * GET /auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser user) {

        UserResponse response = authService.getCurrentUser(user.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Update user profile
     * PUT /auth/profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest httpRequest) {

        log.info("PUT /auth/profile - Request: userId={}, name={}, email={}",
                user.getUserId(), request.getName(), request.getEmail());

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        UserResponse response = authService.updateProfile(
                user.getUserId(),
                request.getName(),
                request.getEmail(),
                ipAddress,
                userAgent);

        log.info("PUT /auth/profile - Response: success=true");
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Mask phone number for logging (show first 5 and last 2 chars)
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return "****";
        }
        return phone.substring(0, 5) + "****" + phone.substring(phone.length() - 2);
    }

    /**
     * Mask email for logging (show first 3 chars of local part + domain)
     */
    private String maskEmail(String email) {
        if (email == null || email.length() < 5) {
            return "****";
        }
        int atIndex = email.indexOf("@");
        if (atIndex < 1) {
            return "****";
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        String maskedLocal = local.length() <= 3
                ? local.charAt(0) + "***"
                : local.substring(0, 3) + "***";
        return maskedLocal + domain;
    }
}
