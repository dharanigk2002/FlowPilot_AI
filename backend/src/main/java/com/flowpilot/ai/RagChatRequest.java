package com.flowpilot.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RagChatRequest(
        @NotBlank @Size(max = 1000) String question
) {
}
