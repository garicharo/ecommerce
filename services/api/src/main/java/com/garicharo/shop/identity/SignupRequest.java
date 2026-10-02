package com.garicharo.shop.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "email is required") String email,
        @NotBlank(message = "password is required")
        @Size(min = 8, max = 128, message = "password must be 8-128 characters")
        String password,
        @NotBlank(message = "displayName is required")
        @Size(max = 120, message = "displayName must be at most 120 characters")
        String displayName) {
}
