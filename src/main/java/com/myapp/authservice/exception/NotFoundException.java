package com.myapp.authservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Not Found Exception (404)
 * Matches Node.js NotFoundError
 */
public class NotFoundException extends BaseException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }
}
