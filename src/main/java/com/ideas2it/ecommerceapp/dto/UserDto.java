package com.ideas2it.ecommerceapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserDto(
    Long id,

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    String email,

    @Size(max = 50, message = "First name must be less than 50 characters")
    String firstName,

    @Size(max = 50, message = "Last name must be less than 50 characters")
    String lastName,

    @Size(max = 255, message = "Address must be less than 255 characters")
    String address,

    @Size(max = 20, message = "Phone number must be less than 20 characters")
    String phone,

    Set<String> roles
) {
    // Compact canonical constructor for validation
    public UserDto {
        if (username != null && username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        if (email != null && email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
    }
}
