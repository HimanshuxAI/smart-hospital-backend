package com.smarthospital.auth;

import com.smarthospital.user.UserRole;
import java.time.LocalDate;

public record CurrentUserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        boolean active,
        PatientProfileSummary patientProfile,
        DoctorProfileSummary doctorProfile) {

    @Override
    public String toString() {
        return "CurrentUserResponse[id=" + id + ", role=" + role + ", active=" + active
                + ", details=REDACTED]";
    }

    public record PatientProfileSummary(
            Long id,
            LocalDate dateOfBirth,
            String gender,
            String address,
            String emergencyContact) {

        @Override
        public String toString() {
            return "PatientProfileSummary[id=" + id + ", details=REDACTED]";
        }
    }

    public record DoctorProfileSummary(
            Long id,
            Long departmentId,
            String departmentName,
            String specialty,
            String licenseNumber,
            int averageConsultationMinutes,
            boolean active) {

        @Override
        public String toString() {
            return "DoctorProfileSummary[id=" + id + ", details=REDACTED]";
        }
    }
}
