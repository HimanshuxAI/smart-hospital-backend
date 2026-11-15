package com.smarthospital.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.smarthospital.appointment.Appointment;
import com.smarthospital.doctor.DoctorProfile;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class QueueEntryLifecycleTest {

    private static final Instant ARRIVED_AT = Instant.parse("2030-01-15T08:00:00Z");
    private static final Instant CALLED_AT = Instant.parse("2030-01-15T08:05:00Z");
    private static final Instant SERVED_AT = Instant.parse("2030-01-15T08:10:00Z");
    private static final Instant COMPLETED_AT = Instant.parse("2030-01-15T08:25:00Z");

    private final Appointment appointment = mock(Appointment.class);
    private final DoctorProfile doctor = mock(DoctorProfile.class);

    @Test
    void waitingEntryCanBeCalledAtomically() {
        QueueEntry entry = waitingEntry();

        entry.call(CALLED_AT);

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.CALLED);
        assertThat(entry.getCalledAt()).isEqualTo(CALLED_AT);
        assertThat(entry.getServedAt()).isNull();
        assertThat(entry.getCompletedAt()).isNull();
        assertThat(entry.getArrivedAt()).isEqualTo(ARRIVED_AT);
    }

    @Test
    void calledEntryCanStartConsultationAtomically() {
        QueueEntry entry = calledEntry();

        entry.startConsultation(SERVED_AT);

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.IN_CONSULTATION);
        assertThat(entry.getCalledAt()).isEqualTo(CALLED_AT);
        assertThat(entry.getServedAt()).isEqualTo(SERVED_AT);
        assertThat(entry.getCompletedAt()).isNull();
    }

    @Test
    void inConsultationEntryCanBeCompletedAtomically() {
        QueueEntry entry = inConsultationEntry();

        entry.complete(COMPLETED_AT);

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.COMPLETED);
        assertThat(entry.getCalledAt()).isEqualTo(CALLED_AT);
        assertThat(entry.getServedAt()).isEqualTo(SERVED_AT);
        assertThat(entry.getCompletedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    void waitingEntryCanBeSkippedAndReturnedToWaiting() {
        QueueEntry entry = waitingEntry();

        entry.skip();
        assertThat(entry.getStatus()).isEqualTo(QueueStatus.SKIPPED);

        entry.returnToWaiting();
        assertThat(entry.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(entry.getArrivedAt()).isEqualTo(ARRIVED_AT);
        assertThat(entry.getCalledAt()).isNull();
        assertThat(entry.getServedAt()).isNull();
        assertThat(entry.getCompletedAt()).isNull();
    }

    @Test
    void waitingAndCalledEntriesCanBeCancelled() {
        QueueEntry waiting = waitingEntry();
        waiting.cancel();
        assertThat(waiting.getStatus()).isEqualTo(QueueStatus.CANCELLED);

        QueueEntry called = calledEntry();
        called.cancel();
        assertThat(called.getStatus()).isEqualTo(QueueStatus.CANCELLED);
        assertThat(called.getCalledAt()).isEqualTo(CALLED_AT);
    }

    @Test
    void invalidTransitionLeavesStatusAndTimestampsUnchanged() {
        QueueEntry entry = waitingEntry();

        assertThatThrownBy(() -> entry.startConsultation(SERVED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("WAITING");

        assertThat(entry.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(entry.getArrivedAt()).isEqualTo(ARRIVED_AT);
        assertThat(entry.getCalledAt()).isNull();
        assertThat(entry.getServedAt()).isNull();
        assertThat(entry.getCompletedAt()).isNull();
    }

    @Test
    void terminalAndSkippedEntriesCannotRestartTheirLifecycle() {
        QueueEntry completed = inConsultationEntry();
        completed.complete(COMPLETED_AT);

        assertThatThrownBy(() -> completed.cancel())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");
        assertThatThrownBy(() -> completed.returnToWaiting())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPLETED");

        QueueEntry skipped = waitingEntry();
        skipped.skip();
        assertThatThrownBy(() -> skipped.call(CALLED_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SKIPPED");

        assertThat(completed.getStatus()).isEqualTo(QueueStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isEqualTo(COMPLETED_AT);
        assertThat(skipped.getStatus()).isEqualTo(QueueStatus.SKIPPED);
    }

    @Test
    void genericPublicStatusMutationAndTimestampSettersAreNotExposed() {
        assertThat(QueueEntry.class.getMethods())
                .noneMatch(method -> method.getName().equals("transitionTo")
                        || method.getName().equals("setStatus"));
        assertThat(QueueEntry.class.getMethods())
                .noneMatch(method -> method.getName().equals("setCalledAt")
                        || method.getName().equals("setServedAt")
                        || method.getName().equals("setCompletedAt"));
    }

    private QueueEntry waitingEntry() {
        return new QueueEntry(
                appointment,
                doctor,
                LocalDate.of(2030, 1, 15),
                1,
                EmergencyTier.NORMAL,
                ARRIVED_AT);
    }

    private QueueEntry calledEntry() {
        QueueEntry entry = waitingEntry();
        entry.call(CALLED_AT);
        return entry;
    }

    private QueueEntry inConsultationEntry() {
        QueueEntry entry = calledEntry();
        entry.startConsultation(SERVED_AT);
        return entry;
    }
}
