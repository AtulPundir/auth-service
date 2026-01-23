package com.myapp.authservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting Configuration using Bucket4j with Redis backend
 * This provides business-logic level rate limiting (Layer 2)
 *
 * Layer 1 (Nginx): IP-based rate limiting
 * Layer 2 (This): Phone/User-based rate limiting + business logic
 */
@Configuration
public class RateLimitingConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedisClient redisClient() {
        String redisUrl;
        if (redisPassword != null && !redisPassword.isBlank()) {
            redisUrl = String.format("redis://%s@%s:%d", redisPassword, redisHost, redisPort);
        } else {
            redisUrl = String.format("redis://%s:%d", redisHost, redisPort);
        }
        return RedisClient.create(redisUrl);
    }

    @Bean
    public StatefulRedisConnection<String, byte[]> redisConnection(RedisClient redisClient) {
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public ProxyManager<String> proxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                        Duration.ofMinutes(10)))
                .build();
    }

    /**
     * Configuration for OTP send rate limiting
     * Limit: 3 OTPs per phone number per 10 minutes
     * This prevents SMS bombing attacks
     */
    @Bean
    public BucketConfiguration otpSendConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(3)
                        .refillGreedy(3, Duration.ofMinutes(10))
                        .build())
                .build();
    }

    /**
     * Configuration for OTP verify rate limiting
     * Limit: 5 attempts per phone number per 5 minutes
     * This prevents brute force OTP guessing
     */
    @Bean
    public BucketConfiguration otpVerifyConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofMinutes(5))
                        .build())
                .build();
    }

    /**
     * Configuration for passkey login rate limiting
     * Limit: 5 attempts per phone number per 5 minutes
     * This prevents brute force passkey guessing
     */
    @Bean
    public BucketConfiguration passkeyLoginConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofMinutes(5))
                        .build())
                .build();
    }
}
