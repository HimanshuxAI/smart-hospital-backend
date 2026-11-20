package com.smarthospital.doctor;

public record DirectoryDoctorResponse(
        Long id,
        Long userId,
        String fullName,
        Long departmentId,
        String departmentName,
        String specialty,
        int averageConsultationMinutes) {
}
