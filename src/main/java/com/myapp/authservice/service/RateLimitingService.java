package com.myapp.authservice.service;

import com.myapp.authservice.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Rate Limiting Service using Bucket4j
 * Provides business-logic level rate limiting based on phone numbers
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitingService {

    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration otpSendConfiguration;
    private final BucketConfiguration otpVerifyConfiguration;
    private final BucketConfiguration passkeyLoginConfiguration;

    private static final String OTP_SEND_PREFIX = "rate:otp:send:";
    private static final String OTP_VERIFY_PREFIX = "rate:otp:verify:";
    private static final String PASSKEY_LOGIN_PREFIX = "rate:passkey:login:";

    /**
     * Check OTP send rate limit
     * @param phone Phone number to check
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkOtpSendLimit(String phone) {
        checkLimit(OTP_SEND_PREFIX + phone, () -> otpSendConfiguration,
                "Too many OTP requests. Please wait before requesting another OTP.");
    }

    /**
     * Check OTP verify rate limit
     * @param phone Phone number to check
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkOtpVerifyLimit(String phone) {
        checkLimit(OTP_VERIFY_PREFIX + phone, () -> otpVerifyConfiguration,
                "Too many verification attempts. Please wait before trying again.");
    }

    /**
     * Check passkey login rate limit
     * @param phone Phone number to check
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkPasskeyLoginLimit(String phone) {
        checkLimit(PASSKEY_LOGIN_PREFIX + phone, () -> passkeyLoginConfiguration,
                "Too many login attempts. Please wait before trying again.");
    }

    /**
     * Reset OTP verify rate limit on successful verification
     * This allows the user to try again after successful OTP entry
     */
    public void resetOtpVerifyLimit(String phone) {
        // Create a new bucket (which resets the limit)
        String key = OTP_VERIFY_PREFIX + phone;
        proxyManager.builder().build(key, () -> otpVerifyConfiguration);
        log.debug("Reset OTP verify rate limit for phone: {}", maskPhone(phone));
    }

    /**
     * Reset passkey login rate limit on successful login
     */
    public void resetPasskeyLoginLimit(String phone) {
        String key = PASSKEY_LOGIN_PREFIX + phone;
        proxyManager.builder().build(key, () -> passkeyLoginConfiguration);
        log.debug("Reset passkey login rate limit for phone: {}", maskPhone(phone));
    }

    private void checkLimit(String key, Supplier<BucketConfiguration> configSupplier, String errorMessage) {
        Bucket bucket = proxyManager.builder().build(key, configSupplier);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            log.warn("Rate limit exceeded for key: {}, retry after: {} seconds", key, waitSeconds);
            throw new RateLimitExceededException(errorMessage, waitSeconds);
        }

        log.debug("Rate limit check passed for key: {}, remaining: {}", key, probe.getRemainingTokens());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }
}
