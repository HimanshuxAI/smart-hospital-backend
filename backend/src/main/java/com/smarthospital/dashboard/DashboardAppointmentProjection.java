package com.smarthospital.dashboard;

import com.smarthospital.appointment.AppointmentStatus;
import java.time.Instant;

public interface DashboardAppointmentProjection {

    Long getAppointmentId();

    String getDoctorName();

    String getSpecialty();

    String getPatientName();

    Instant getStartsAt();

    Instant getEndsAt();

    AppointmentStatus getStatus();

    String getVisitReason();
}
