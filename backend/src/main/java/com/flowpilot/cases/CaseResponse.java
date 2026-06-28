package com.flowpilot.cases;

import java.math.BigDecimal;
import java.time.Instant;

public record CaseResponse(
        Long id,
        String title,
        String description,
        String customerName,
        String customerEmail,
        CustomerTier customerTier,
        String orderReference,
        BigDecimal orderValue,
        CasePriority priority,
        CaseStatus status,
        CreatorSummary createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public record CreatorSummary(Long id, String email, String displayName) {
    }
}
