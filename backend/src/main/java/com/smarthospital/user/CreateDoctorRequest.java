package com.smarthospital.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDoctorRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must contain at least 8 characters")
        @Size(max = 72, message = "Password must not exceed 72 characters")
        String password,
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must not exceed 120 characters")
        String fullName,
        @NotBlank(message = "Phone is required")
        @Size(max = 32, message = "Phone must not exceed 32 characters")
        String phone,
        @NotNull(message = "Department is required")
        Long departmentId,
        @NotBlank(message = "Specialty is required")
        @Size(max = 120, message = "Specialty must not exceed 120 characters")
        String specialty,
        @NotBlank(message = "License number is required")
        @Size(max = 64, message = "License number must not exceed 64 characters")
        String licenseNumber,
        @Min(value = 1, message = "Average consultation minutes must be positive")
        @Max(value = 240, message = "Average consultation minutes must not exceed 240")
        int averageConsultationMinutes) {

    @Override
    public String toString() {
        return "CreateDoctorRequest[password=REDACTED]";
    }
}
