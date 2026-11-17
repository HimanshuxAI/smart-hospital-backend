package com.smarthospital.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smarthospital.common.ApiException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class QueueServiceTest extends QueueTestSupport {

    @Autowired
    private QueueService queueService;

    @Test
    void serviceBoundaryRejectsPatientMutation() {
        TestPatient patient = createPatient();
        TestDoctor doctor = createDoctor();
        QueueEntry entry = createQueueEntry(
                patient,
                doctor,
                EmergencyTier.NORMAL,
                Instant.parse("2030-01-15T08:00:00Z"));

        assertThatThrownBy(() -> queueService.classify(
                patient.userId(),
                entry.getId(),
                new ClassifyQueueEntryRequest(EmergencyTier.HIGH, "Patient attempt")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("FORBIDDEN");
                });
    }

    @Test
    void doctorCannotListAnotherDoctorsQueue() {
        TestDoctor doctor = createDoctor();
        TestDoctor otherDoctor = createDoctor();

        assertThatThrownBy(() -> queueService.list(
                doctor.userId(),
                otherDoctor.doctorId(),
                TODAY))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(403);
                    assertThat(exception.code()).isEqualTo("FORBIDDEN");
                });
    }

    @Test
    void administratorQueueQueryRequiresDoctorScope() {
        TestAdmin admin = createAdmin();

        assertThatThrownBy(() -> queueService.list(admin.userId(), null, TODAY))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status().value()).isEqualTo(400);
                    assertThat(exception.code()).isEqualTo("DOCTOR_SCOPE_REQUIRED");
                });
    }
}
