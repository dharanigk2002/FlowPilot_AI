package com.flowpilot.auth;

import com.flowpilot.user.UserRole;

public record AuthResponse(
        long expiresInSeconds,
        UserSummary user
) {
    public record UserSummary(Long id, String email, String displayName, UserRole role) {
    }
}
