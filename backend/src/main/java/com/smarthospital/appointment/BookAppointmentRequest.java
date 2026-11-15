package com.smarthospital.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BookAppointmentRequest(
        @NotNull Long slotId,
        Long doctorId,
        @NotBlank @Size(max = 500) String visitReason) {
}
