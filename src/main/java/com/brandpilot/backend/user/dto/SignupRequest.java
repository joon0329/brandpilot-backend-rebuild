package com.brandpilot.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record SignupRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotNull
        @Size(min = 8, max = 64)
        String password,

        @NotBlank
        @Size(max = 50)
        String name
) {
    public SignupRequest {
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }

        if (name != null) {
            name = name.strip();
        }
    }
}
