package com.jobtracker.main.jobapplications;

import java.time.Instant;

public record ChangeStatusResponseDto(JobApplicationStatus status, StatusChangeCause cause, String detail, Instant createdAt) {
}
