package com.myapp.authservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Update Profile Request DTO
 * All fields are optional
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @Email(message = "Please provide a valid email address")
    private String email;
}
