package com.myapp.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple Message Response DTO
 * Matches Node.js { message: string } return type
 *
 * Used by setPasskey, logout endpoints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private String message;

    public static MessageResponse of(String message) {
        return new MessageResponse(message);
    }
}
