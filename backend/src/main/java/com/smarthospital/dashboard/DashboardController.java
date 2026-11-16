package com.smarthospital.dashboard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(tags = "Patient", summary = "Get the authenticated patient's dashboard")
    @SecurityRequirement(name = "bearerAuth")
    public DashboardService.PatientDashboardResponse patientDashboard(
            @AuthenticationPrincipal Jwt jwt) {
        return dashboardService.patientDashboard(Long.valueOf(jwt.getSubject()));
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(tags = "Doctor", summary = "Get the authenticated doctor's dashboard")
    @SecurityRequirement(name = "bearerAuth")
    public DashboardService.DoctorDashboardResponse doctorDashboard(
            @AuthenticationPrincipal Jwt jwt) {
        return dashboardService.doctorDashboard(Long.valueOf(jwt.getSubject()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(tags = "Admin", summary = "Get administrative dashboard totals")
    @SecurityRequirement(name = "bearerAuth")
    public DashboardService.AdminDashboardResponse adminDashboard(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dashboardService.adminDashboard(date);
    }
}
