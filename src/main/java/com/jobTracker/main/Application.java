package com.jobtracker.main;

import java.util.UUID;

public record Application(String company, String position, ApplicationStatus status, String notes,
                          String links, UUID id) {
}
