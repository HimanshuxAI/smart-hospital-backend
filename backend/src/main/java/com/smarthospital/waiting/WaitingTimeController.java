package com.smarthospital.waiting;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/queue")
@PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
public class WaitingTimeController {

    private final WaitingTimeService waitingTimeService;

    public WaitingTimeController(WaitingTimeService waitingTimeService) {
        this.waitingTimeService = waitingTimeService;
    }

    @GetMapping("/{entryId}/waiting-time")
    @Operation(
            tags = {"Patient", "Doctor", "Admin"},
            summary = "Predict waiting time for an authorized queue entry")
    @SecurityRequirement(name = "bearerAuth")
    public WaitingTimeResponse getWaitingTime(
            @PathVariable Long entryId,
            @AuthenticationPrincipal Jwt jwt) {
        return waitingTimeService.getWaitingTime(
                entryId,
                Long.valueOf(jwt.getSubject()));
    }
}
