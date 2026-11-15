package com.smarthospital.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDoctorProfileRequest(
        @NotNull(message = "Department is required")
        Long departmentId,
        @NotBlank(message = "Specialty is required")
        @Size(max = 120, message = "Specialty must not exceed 120 characters")
        String specialty,
        @Min(value = 1, message = "Average consultation minutes must be positive")
        @Max(value = 240, message = "Average consultation minutes must not exceed 240")
        int averageConsultationMinutes) {
}
