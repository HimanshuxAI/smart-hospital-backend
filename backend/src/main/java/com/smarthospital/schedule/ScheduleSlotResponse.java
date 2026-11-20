package com.smarthospital.schedule;

import java.time.Instant;

public record ScheduleSlotResponse(
        Long id,
        Long doctorId,
        Instant startsAt,
        Instant endsAt,
        String reasonLabel,
        ScheduleSlotStatus status) {
}
