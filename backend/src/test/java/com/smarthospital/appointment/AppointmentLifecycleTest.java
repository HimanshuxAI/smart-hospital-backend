package com.smarthospital.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.user.User;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AppointmentLifecycleTest {

    private static final Instant CHECKED_IN_AT = Instant.parse("2030-01-15T08:00:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2030-01-15T08:15:00Z");
    private static final Instant CANCELLED_AT = Instant.parse("2030-01-15T07:45:00Z");
    private static final Instant NO_SHOW_AT = Instant.parse("2030-01-15T08:30:00Z");

    private final PatientProfile patient = mock(PatientProfile.class);
    private final DoctorProfile doctor = mock(DoctorProfile.class);
    private final ScheduleSlot slot = mock(ScheduleSlot.class);

    @Test
    void bookedAppointmentCanCheckInAtomically() {
        Appointment appointment = bookedAppointment();

        appointment.checkIn(CHECKED_IN_AT);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CHECKED_IN);
        assertThat(appointment.getActiveMarker()).isEqualTo(1);
        assertThat(appointment.getCheckedInAt()).isEqualTo(CHECKED_IN_AT);
    }

    @Test
    void bookedAppointmentCanBeCancelledAtomically() {
        Appointment appointment = bookedAppointment();
        User actor = mock(User.class);

        appointment.cancel(actor, "Patient requested cancellation", CANCELLED_AT);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getActiveMarker()).isNull();
        assertThat(appointment.getCancelledBy()).isSameAs(actor);
        assertThat(appointment.getCancellationReason()).isEqualTo("Patient requested cancellation");
        assertThat(appointment.getCancelledAt()).isEqualTo(CANCELLED_AT);
    }

    @Test
    void bookedAppointmentCanBeMarkedNoShowAtomically() {
        Appointment appointment = bookedAppointment();

        appointment.markNoShow(NO_SHOW_AT);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
        assertThat(appointment.getActiveMarker()).isNull();
        assertThat(appointment.getNoShowAt()).isEqualTo(NO_SHOW_AT);
    }

    @Test
    void checkedInAppointmentCanBeCompletedAtomically() {
        Appointment appointment = bookedAppointment();
        appointment.checkIn(CHECKED_IN_AT);

        appointment.complete(COMPLETED_AT);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getActiveMarker()).isNull();
        assertThat(appointment.getCheckedInAt()).isEqualTo(CHECKED_IN_AT);
        assertThat(appointment.getCompletedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void invalidTransitionLeavesStatusMarkerAndTimestampsUnchanged() {
        Appointment appointment = bookedAppointment();
        appointment.cancel(mock(User.class), "Cancelled once", CANCELLED_AT);

        assertThatThrownBy(() -> appointment.checkIn(CHECKED_IN_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getActiveMarker()).isNull();
        assertThat(appointment.getCancelledAt()).isEqualTo(CANCELLED_AT);
        assertThat(appointment.getCheckedInAt()).isNull();
        assertThat(appointment.getCompletedAt()).isNull();
        assertThat(appointment.getNoShowAt()).isNull();
    }

    @Test
    void checkedInAppointmentCannotBeCancelledOrRecheckedIn() {
        Appointment appointment = bookedAppointment();
        appointment.checkIn(CHECKED_IN_AT);

        assertThatThrownBy(() -> appointment.cancel(
                        mock(User.class),
                        "Too late to cancel",
                        CANCELLED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHECKED_IN");
        assertThatThrownBy(() -> appointment.checkIn(CHECKED_IN_AT.plusSeconds(60)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHECKED_IN");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CHECKED_IN);
        assertThat(appointment.getActiveMarker()).isEqualTo(1);
        assertThat(appointment.getCheckedInAt()).isEqualTo(CHECKED_IN_AT);
        assertThat(appointment.getCancelledAt()).isNull();
    }

    @Test
    void genericPublicStatusMutationIsNotExposed() {
        assertThat(Appointment.class.getMethods())
                .noneMatch(method -> method.getName().equals("transitionTo")
                        || method.getName().equals("setStatus")
                        || method.getName().equals("setActiveMarker")
                        || method.getName().equals("setCheckedInAt")
                        || method.getName().equals("setCompletedAt")
                        || method.getName().equals("setNoShowAt")
                        || method.getName().equals("setCancelledAt"));
    }

    private Appointment bookedAppointment() {
        return new Appointment(
                patient,
                doctor,
                slot,
                LocalDate.of(2030, 1, 15),
                "Synthetic lifecycle visit");
    }
}
