package com.flowpilot.auth;

import com.flowpilot.user.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 2, max = 120) String displayName,
        @NotBlank @Size(min = 8, max = 100) String password,
        @NotNull UserRole role
) {
}
