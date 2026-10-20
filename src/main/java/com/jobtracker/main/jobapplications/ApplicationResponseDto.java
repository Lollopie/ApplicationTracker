package com.jobtracker.main.jobapplications;

import java.util.List;
import java.util.UUID;

public record ApplicationResponseDto(UUID id, String company, String position, String notes, String links, List<ChangeStatusResponseDto> statusHistory) {
}
