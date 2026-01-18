package com.myapp.authservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * OTP Utilities - matches Node.js OtpUtils behavior exactly
 *
 * Node.js implementation:
 * - generateOtp(): string - generates 6-digit OTP
 * - formatPhone(phone): string - formats to E.164
 * - getExpiryDate(): Date - returns expiry timestamp
 * - getRedisKey(phone): string - generates Redis key
 */
@Slf4j
@Component
public class OtpUtils {

    private static final SecureRandom random = new SecureRandom();
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    private final int otpLength;
    private final int expiryMinutes;

    public OtpUtils(
            @Value("${app.otp.length}") int otpLength,
            @Value("${app.otp.expiry-minutes}") int expiryMinutes) {
        this.otpLength = otpLength;
        this.expiryMinutes = expiryMinutes;
    }

    /**
     * Generate random OTP
     * Matches: OtpUtils.generateOtp()
     *
     * @return OTP string (e.g., "123456")
     */
    public String generateOtp() {
        int max = (int) Math.pow(10, otpLength);
        int otp = random.nextInt(max);
        return String.format("%0" + otpLength + "d", otp);
    }

    /**
     * Format phone number to E.164 format
     * Matches: OtpUtils.formatPhone(phone)
     *
     * E.164 format: +[country code][number]
     * Example: +919876543210
     *
     * @param phone Phone number (with or without +)
     * @return Formatted phone number
     */
    public String formatPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number is required");
        }

        // Remove whitespace and dashes
        String cleaned = phone.replaceAll("[\\s-]", "");

        // Add + if not present
        if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }

        // Validate E.164 format
        if (!PHONE_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException("Invalid phone number format. Use E.164 format (e.g., +919876543210)");
        }

        return cleaned;
    }

    /**
     * Get OTP expiry date/time
     * Matches: OtpUtils.getExpiryDate()
     *
     * @return Expiry timestamp
     */
    public LocalDateTime getExpiryDate() {
        return LocalDateTime.now().plusMinutes(expiryMinutes);
    }

    /**
     * Generate Redis key for OTP storage
     * Matches: OtpUtils.getRedisKey(phone)
     *
     * Format: otp:{phone}
     * Example: otp:+919876543210
     *
     * @param phone Formatted phone number
     * @return Redis key
     */
    public String getRedisKey(String phone) {
        return "otp:" + phone;
    }

    /**
     * Validate phone number format
     *
     * @param phone Phone number
     * @return true if valid E.164 format
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Get expiry time in seconds (for Redis TTL)
     *
     * @return Expiry seconds
     */
    public long getExpirySeconds() {
        return expiryMinutes * 60L;
    }
}
