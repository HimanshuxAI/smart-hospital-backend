package com.smarthospital.appointment;

import com.smarthospital.queue.QueueCheckInResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    public AppointmentResponse book(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BookAppointmentRequest request) {
        return appointmentService.book(Long.valueOf(jwt.getSubject()), request);
    }

    @PostMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    public AppointmentResponse cancel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long appointmentId,
            @Valid @RequestBody CancelAppointmentRequest request) {
        return appointmentService.cancel(
                Long.valueOf(jwt.getSubject()),
                appointmentId,
                request.reason());
    }

    @PostMapping("/{appointmentId}/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public QueueCheckInResponse checkIn(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long appointmentId) {
        return appointmentService.checkIn(Long.valueOf(jwt.getSubject()), appointmentId);
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long appointmentId) {
        return appointmentService.get(Long.valueOf(jwt.getSubject()), appointmentId);
    }
}
