package com.myapp.authservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Client for calling notification-service internal endpoints.
 * Used to send OTP via SMS/Email after generation.
 */
@Component
public class NotificationServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceClient.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final RestClient restClient;
    private final String apiKey;

    public NotificationServiceClient(
            @Value("${app.notification-service.base-url}") String baseUrl,
            @Value("${app.notification-service.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
    }

    /**
     * Send OTP via SMS.
     *
     * @param phone E.164 formatted phone number (e.g., +919876543210)
     * @param otp   The OTP code
     * @param ttlMinutes Time to live in minutes
     */
    public void sendOtpSms(String phone, String otp, int ttlMinutes) {
        try {
            Map<String, Object> request = Map.of(
                    "templateKey", "OTP_SMS",
                    "channel", "SMS",
                    "recipient", phone,
                    "data", Map.of(
                            "otp", otp,
                            "ttl", String.valueOf(ttlMinutes)
                    ),
                    "priority", "HIGH",
                    "idempotencyKey", "OTP_SMS:" + phone + ":" + System.currentTimeMillis()
            );

            restClient.post()
                    .uri("/internal/notifications/send")
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("OTP SMS sent successfully to: {}", maskRecipient(phone));
        } catch (Exception e) {
            // Log but don't fail the OTP generation — best effort delivery
            logger.error("Failed to send OTP SMS to {}: {}", maskRecipient(phone), e.getMessage());
        }
    }

    /**
     * Send OTP via Email.
     *
     * @param email    Email address
     * @param otp      The OTP code
     * @param ttlMinutes Time to live in minutes
     */
    public void sendOtpEmail(String email, String otp, int ttlMinutes) {
        try {
            Map<String, Object> request = Map.of(
                    "templateKey", "OTP_EMAIL",
                    "channel", "EMAIL",
                    "recipient", email,
                    "data", Map.of(
                            "otp", otp,
                            "ttl", String.valueOf(ttlMinutes)
                    ),
                    "priority", "HIGH",
                    "idempotencyKey", "OTP_EMAIL:" + email + ":" + System.currentTimeMillis()
            );

            restClient.post()
                    .uri("/internal/notifications/send")
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("OTP Email sent successfully to: {}", maskRecipient(email));
        } catch (Exception e) {
            // Log but don't fail the OTP generation — best effort delivery
            logger.error("Failed to send OTP Email to {}: {}", maskRecipient(email), e.getMessage());
        }
    }

    private String maskRecipient(String recipient) {
        if (recipient == null || recipient.length() < 4) return "****";
        if (recipient.contains("@")) {
            int atIndex = recipient.indexOf("@");
            return recipient.substring(0, Math.min(3, atIndex)) + "***" + recipient.substring(atIndex);
        }
        return recipient.substring(0, 4) + "****" + recipient.substring(recipient.length() - 2);
    }
}
