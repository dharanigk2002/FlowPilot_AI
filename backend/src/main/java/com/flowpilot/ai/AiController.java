package com.flowpilot.ai;

import com.flowpilot.common.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AiController {

    private final RagService ragService;

    public AiController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/chat")
    public RagChatResponse chat(@Valid @RequestBody RagChatRequest request) {
        return ragService.answer(request);
    }
}
