package com.smarthospital.schedule;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateScheduleSlotRequest(
        @NotNull Long doctorId,
        @NotNull Instant startsAt,
        @NotNull Instant endsAt,
        @Size(max = 255) String reasonLabel) {
}
