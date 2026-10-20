package com.jobtracker.main.jobapplications;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record JobApplicationDto(
        @NotBlank(message = "Company is required")
        String company,
        @NotBlank(message = "Position is required")
        String position,
        @NotNull(message = "Status is required")
        JobApplicationStatus status,
        @Length(min = 0, max = 500)
        String notes,
        @Length(min = 0, max = 500)
        String links) {
}
