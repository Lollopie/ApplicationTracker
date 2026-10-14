package com.jobtracker.main;

import java.time.Instant;

public record ChangeStatusResponseDto(ApplicationStatus status, StatusChangeCause cause, String detail, Instant createdAt) {
}
