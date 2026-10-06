package com.jobtracker.main;

public record ApplicationDto(String company, String position, ApplicationStatus status, String notes, String links) {
}
