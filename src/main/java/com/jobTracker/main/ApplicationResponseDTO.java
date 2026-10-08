package com.jobtracker.main;

import java.util.UUID;

public record ApplicationResponseDto(UUID id, String company, String position, ApplicationStatus status, String notes, String links) {
}
