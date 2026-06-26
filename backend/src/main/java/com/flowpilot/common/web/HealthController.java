package com.flowpilot.common.web;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", "flowpilot-backend", Instant.now());
    }

    public record HealthResponse(String status, String service, Instant timestamp) {
    }
}
