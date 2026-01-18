package com.myapp.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Authentication Service - Java 25 Spring Boot Implementation
 * Feature-parity migration from Node.js + TypeScript auth-service
 *
 * Key Features:
 * - Phone-based OTP authentication
 * - Passkey (PIN) authentication
 * - JWT access & refresh tokens
 * - Redis-backed OTP caching
 * - Comprehensive audit logging
 * - Role-based access control
 * - Automatic token cleanup
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
