package com.flowpilot.auth;

import com.flowpilot.user.UserRole;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserSummary user
) {

    public static AuthResponse bearer(String accessToken, long expiresInSeconds, UserSummary user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }

    public record UserSummary(Long id, String email, String displayName, UserRole role) {
    }
}
