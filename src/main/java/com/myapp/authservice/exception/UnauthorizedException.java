package com.myapp.authservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Unauthorized Exception (401)
 * Matches Node.js UnauthorizedError
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
