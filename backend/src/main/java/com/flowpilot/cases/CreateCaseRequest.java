package com.flowpilot.cases;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateCaseRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotBlank @Size(max = 120) String customerName,
        @Email @Size(max = 320) String customerEmail,
        @NotNull CustomerTier customerTier,
        @Size(max = 80) String orderReference,
        @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal orderValue,
        @NotNull CasePriority priority
) {
}
