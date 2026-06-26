package com.flowpilot.auth;

import com.flowpilot.user.UserRole;

public record CurrentUserResponse(
        Long id,
        String email,
        String displayName,
        UserRole role
) {
}
