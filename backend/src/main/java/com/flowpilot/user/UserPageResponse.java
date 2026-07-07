package com.flowpilot.user;

import java.util.List;

public record UserPageResponse(
        List<UserResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
