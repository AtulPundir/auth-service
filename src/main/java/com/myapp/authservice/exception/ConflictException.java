package com.myapp.authservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Conflict Exception (409)
 * Matches Node.js ConflictError
 */
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }
}
