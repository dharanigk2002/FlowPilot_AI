package com.flowpilot.cases;

import jakarta.validation.constraints.NotNull;

public record UpdateCaseStatusRequest(@NotNull CaseStatus status) {
}
