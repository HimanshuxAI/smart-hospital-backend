package com.smarthospital.dashboard;

import com.smarthospital.queue.EmergencyTier;
import com.smarthospital.queue.QueueStatus;
import java.time.Instant;
import java.time.LocalDate;

public interface DashboardQueueProjection {

    Long getEntryId();

    Long getAppointmentId();

    String getDoctorName();

    String getPatientName();

    LocalDate getQueueDate();

    Integer getTokenNumber();

    EmergencyTier getEmergencyTier();

    QueueStatus getStatus();

    Instant getArrivedAt();

    Instant getScheduledStart();
}
