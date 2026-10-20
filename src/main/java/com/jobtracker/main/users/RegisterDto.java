package com.jobtracker.main.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record RegisterDto (
        @NotBlank(message = "Username is required.")
        @Length(min = 1, max = 32, message = "Username must be between 1 and 32 characters long.")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only include alpha-numeric characters and ., _ and -.")
        String username,
        @NotBlank(message = "Password is required.")
        @Length(min = 8, max = 64, message = "Password must be between 8 and 64 characters long.")
        String password) {
}
