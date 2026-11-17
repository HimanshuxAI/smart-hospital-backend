package com.smarthospital.appointment;

import java.time.Instant;
import java.time.LocalDate;

public record AppointmentResponse(
        Long id,
        Long patientId,
        Long doctorId,
        Long slotId,
        LocalDate appointmentDate,
        Instant startsAt,
        Instant endsAt,
        String visitReason,
        AppointmentStatus status,
        Instant checkedInAt,
        Instant completedAt,
        Instant cancelledAt,
        String cancellationReason) {
}
