package com.smarthospital.schedule;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/slots")
    public List<ScheduleSlotResponse> listSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return scheduleService.listAdminSlots(doctorId, date);
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
