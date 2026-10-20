package com.jobtracker.main;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record UpdateApplicationDto(
        @NotBlank(message = "Company is required")
        String company,
        @NotBlank(message = "Position is required")
        String position,
        @Length(min = 0, max = 500)
        String notes,
        @Length(min = 0, max = 500)
        String links) {
}
