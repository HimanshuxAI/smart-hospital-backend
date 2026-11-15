package com.smarthospital.user;

public record DoctorProfileResponse(
        Long id,
        Long userId,
        String email,
        String fullName,
        String phone,
        UserRole role,
        boolean active,
        Long departmentId,
        String departmentName,
        String specialty,
        String licenseNumber,
        int averageConsultationMinutes,
        boolean profileActive) {

    @Override
    public String toString() {
        return "DoctorProfileResponse[id=" + id + ", userId=" + userId + ", role=" + role
                + ", active=" + active + ", departmentId=" + departmentId
                + ", averageConsultationMinutes=" + averageConsultationMinutes
                + ", profileActive=" + profileActive + ", details=REDACTED]";
    }
}
