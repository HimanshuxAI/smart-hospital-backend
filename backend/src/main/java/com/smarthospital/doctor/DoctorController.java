package com.smarthospital.doctor;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public List<DirectoryDoctorResponse> listActiveDoctors() {
        return doctorService.listActiveDoctors();
    }

    @GetMapping("/{doctorId}")
    public DirectoryDoctorResponse getActiveDoctor(@PathVariable Long doctorId) {
        return doctorService.getActiveDoctor(doctorId);
    }
}
