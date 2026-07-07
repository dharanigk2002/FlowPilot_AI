package com.flowpilot.user;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String displayName,
        UserRole role,
        Instant createdAt,
        Instant updatedAt
) {
}
