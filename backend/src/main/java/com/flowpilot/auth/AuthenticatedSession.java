package com.flowpilot.auth;

record AuthenticatedSession(
        String accessToken,
        long expiresInSeconds,
        AuthResponse.UserSummary user
) {
    AuthResponse toResponse() {
        return new AuthResponse(expiresInSeconds, user);
    }
}
