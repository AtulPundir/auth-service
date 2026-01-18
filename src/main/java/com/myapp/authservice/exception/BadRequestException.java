package com.myapp.authservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Bad Request Exception (400)
 * Matches Node.js BadRequestError
 */
public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }
}
