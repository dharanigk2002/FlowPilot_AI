package com.flowpilot.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.initial-admin")
public record InitialAdminProperties(
        String email,
        String displayName,
        String password
) {
    boolean isConfigured() {
        return hasText(email) || hasText(password);
    }

    String normalizedEmail() {
        return email.trim().toLowerCase();
    }

    String resolvedDisplayName() {
        return hasText(displayName) ? displayName.trim() : "FlowPilot Admin";
    }

    void validate() {
        if (!isConfigured()) {
            return;
        }
        if (!hasText(email)) {
            throw new IllegalStateException("INITIAL_ADMIN_EMAIL is required when initial admin bootstrap is configured.");
        }
        if (!hasText(password)) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD is required when initial admin bootstrap is configured.");
        }
        if (password.length() < 8) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must contain at least 8 characters.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
