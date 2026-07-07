package com.flowpilot.cases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitAgentRecommendationRequest(
        @NotNull AgentSuggestedAction suggestedAction,
        @NotBlank @Size(min = 10, max = 2_000) String notes
) {
}
