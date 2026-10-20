package com.jobtracker.main.jobapplications;

import jakarta.validation.constraints.NotNull;

public record ChangeStatusDto(
        @NotNull(message = "Status is required")
        ApplicationStatus status) {
}

