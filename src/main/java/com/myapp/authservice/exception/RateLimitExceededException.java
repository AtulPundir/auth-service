package com.myapp.authservice.exception;

import lombok.Getter;

/**
 * Exception thrown when rate limit is exceeded
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String message) {
        super(message);
        this.retryAfterSeconds = 60; // Default 60 seconds
    }
}
