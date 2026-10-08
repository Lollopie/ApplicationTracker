package com.jobtracker.main;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplicationDto(
        @NotBlank(message = "Company is required")
        String company,
        @NotBlank(message = "Position is required")
        String position,
        @NotNull(message = "Status is required")
        ApplicationStatus status,

        String notes,

        String links) {
}
