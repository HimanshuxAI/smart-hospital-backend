package com.smarthospital.schedule;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    public AdminScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleSlotResponse createSlot(@Valid @RequestBody CreateScheduleSlotRequest request) {
        return scheduleService.createSlot(request);
    }

    @PostMapping("/slots/{slotId}/cancel")
    public ScheduleSlotResponse cancelSlot(@PathVariable Long slotId) {
        return scheduleService.cancelSlot(slotId);
    }
}
