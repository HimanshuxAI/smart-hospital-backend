package com.smarthospital.doctor;

import com.smarthospital.common.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorService {

    private final DoctorProfileRepository doctorProfileRepository;

    public DoctorService(DoctorProfileRepository doctorProfileRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<DirectoryDoctorResponse> listActiveDoctors() {
        return doctorProfileRepository.findActiveDirectoryDoctors().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DirectoryDoctorResponse getActiveDoctor(Long doctorId) {
        DoctorProfile doctor = doctorProfileRepository.findActiveDirectoryDoctor(doctorId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_NOT_FOUND",
                        "Doctor was not found"));
        return toResponse(doctor);
    }

    private DirectoryDoctorResponse toResponse(DoctorProfile doctor) {
        return new DirectoryDoctorResponse(
                doctor.getId(),
                doctor.getUser().getId(),
                doctor.getUser().getFullName(),
                doctor.getDepartment().getId(),
                doctor.getDepartment().getName(),
                doctor.getSpecialty(),
                doctor.getAverageConsultationMinutes());
    }
}
