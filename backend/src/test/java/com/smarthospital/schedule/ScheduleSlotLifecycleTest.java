package com.smarthospital.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.smarthospital.doctor.DoctorProfile;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ScheduleSlotLifecycleTest {

    private static final Instant STARTS_AT = Instant.parse("2030-01-15T08:00:00Z");

    private final DoctorProfile doctor = mock(DoctorProfile.class);

    @Test
    void openSlotCanBeBookedAndRemainsActive() {
        ScheduleSlot slot = new ScheduleSlot(doctor, STARTS_AT, STARTS_AT.plusSeconds(900));

        assertThat(slot.getStatus()).isEqualTo(ScheduleSlotStatus.OPEN);
        assertThat(slot.isActive()).isTrue();

        slot.markBooked();

        assertThat(slot.getStatus()).isEqualTo(ScheduleSlotStatus.BOOKED);
        assertThat(slot.isActive()).isTrue();
    }

    @Test
    void openSlotCanBeCancelledAndBecomesInactive() {
        ScheduleSlot slot = new ScheduleSlot(doctor, STARTS_AT, STARTS_AT.plusSeconds(900));

        slot.cancel();

        assertThat(slot.getStatus()).isEqualTo(ScheduleSlotStatus.CANCELLED);
        assertThat(slot.isActive()).isFalse();
    }

    @Test
    void bookedSlotCanBeCancelledAndBecomesInactive() {
        ScheduleSlot slot = new ScheduleSlot(doctor, STARTS_AT, STARTS_AT.plusSeconds(900));
        slot.markBooked();

        slot.cancel();

        assertThat(slot.getStatus()).isEqualTo(ScheduleSlotStatus.CANCELLED);
        assertThat(slot.isActive()).isFalse();
    }

    @Test
    void bookedSlotCanBeCompletedAndBecomesInactive() {
        ScheduleSlot slot = new ScheduleSlot(doctor, STARTS_AT, STARTS_AT.plusSeconds(900));
        slot.markBooked();

        slot.complete();

        assertThat(slot.getStatus()).isEqualTo(ScheduleSlotStatus.COMPLETED);
        assertThat(slot.isActive()).isFalse();
    }

    @Test
    void invalidTransitionsLeaveStatusAndActiveFlagUnchanged() {
        ScheduleSlot open = new ScheduleSlot(doctor, STARTS_AT, STARTS_AT.plusSeconds(900));

        assertThatThrownBy(open::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OPEN");
        assertThat(open.getStatus()).isEqualTo(ScheduleSlotStatus.OPEN);
        assertThat(open.isActive()).isTrue();

        open.markBooked();
        assertThatThrownBy(open::markBooked)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BOOKED");
        assertThat(open.getStatus()).isEqualTo(ScheduleSlotStatus.BOOKED);
        assertThat(open.isActive()).isTrue();

        open.complete();
        assertThatThrownBy(open::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");
        assertThat(open.getStatus()).isEqualTo(ScheduleSlotStatus.COMPLETED);
        assertThat(open.isActive()).isFalse();
    }
}
