package com.smarthospital.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must not exceed 120 characters")
        String fullName,
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must contain at least 8 characters")
        @Size(max = 72, message = "Password must not exceed 72 characters")
        String password,
        @NotBlank(message = "Phone is required")
        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone,
        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,
        @Size(max = 32, message = "Gender must not exceed 32 characters")
        String gender,
        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,
        @Size(max = 160, message = "Emergency contact must not exceed 160 characters")
        String emergencyContact) {

    @Override
    public String toString() {
        return "RegisterRequest[password=REDACTED]";
    }
}
