package com.flowpilot.knowledge;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeSearchRequest(
        @NotBlank @Size(max = 1000) String query,
        @Min(1) @Max(20) Integer topK,
        @DecimalMin("0.0") @DecimalMax("1.0") Double similarityThreshold
) {
}
