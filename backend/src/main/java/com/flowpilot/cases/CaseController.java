package com.flowpilot.cases;

import com.flowpilot.common.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/cases")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseResponse create(@Valid @RequestBody CreateCaseRequest request, Authentication authentication) {
        return caseService.create(request, authentication);
    }

    @GetMapping
    public CasePageResponse findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return caseService.findAll(page, size);
    }

    @GetMapping("/{id}")
    public CaseResponse findById(@PathVariable @Positive Long id) {
        return caseService.findById(id);
    }

    @PatchMapping("/{id}/status")
    public CaseResponse updateStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateCaseStatusRequest request
    ) {
        return caseService.updateStatus(id, request);
    }
}
