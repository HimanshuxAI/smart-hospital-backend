package com.smarthospital.waiting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.smarthospital.appointment.Appointment;
import com.smarthospital.common.ApiException;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.queue.EmergencyTier;
import com.smarthospital.queue.QueueEntry;
import com.smarthospital.queue.QueueEntryRepository;
import com.smarthospital.queue.QueueStatus;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WaitingTimeServiceTest {

    private static final Instant NOW = Instant.parse("2030-01-15T10:00:00Z");
    private static final LocalDate QUEUE_DATE = LocalDate.of(2030, 1, 15);

    @Mock
    private QueueEntryRepository queueEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private WaitingTimeClient waitingTimeClient;

    private WaitingTimeService service;
    private User user;
    private PatientProfile patient;
    private DoctorProfile doctor;
    private Appointment appointment;
    private ScheduleSlot slot;
    private QueueEntry entry;

    @BeforeEach
    void setUp() {
        service = new WaitingTimeService(
                queueEntryRepository,
                userRepository,
                patientProfileRepository,
                doctorProfileRepository,
                waitingTimeClient,
                new WaitingTimeFallback(),
                Clock.fixed(NOW, ZoneId.of("UTC")),
                ZoneId.of("UTC"));

        user = mock(User.class);
        when(user.getId()).thenReturn(44L);

        patient = mock(PatientProfile.class);
        when(patient.getId()).thenReturn(11L);
        when(patient.getUser()).thenReturn(user);

        doctor = mock(DoctorProfile.class);
        when(doctor.getId()).thenReturn(21L);
        when(doctor.getAverageConsultationMinutes()).thenReturn(15);

        slot = mock(ScheduleSlot.class);
        when(slot.getStartsAt()).thenReturn(Instant.parse("2030-01-15T09:30:00Z"));

        appointment = mock(Appointment.class);
        when(appointment.getPatient()).thenReturn(patient);
        when(appointment.getSlot()).thenReturn(slot);

        entry = mock(QueueEntry.class);
        when(entry.getId()).thenReturn(101L);
        when(entry.getAppointment()).thenReturn(appointment);
        when(entry.getDoctor()).thenReturn(doctor);
        when(entry.getQueueDate()).thenReturn(QUEUE_DATE);
        when(entry.getTokenNumber()).thenReturn(1);
        when(entry.getEmergencyTier()).thenReturn(EmergencyTier.NORMAL);
        when(entry.getStatus()).thenReturn(QueueStatus.WAITING);

        QueueEntry second = mock(QueueEntry.class);
        when(second.getId()).thenReturn(102L);
        when(second.getEmergencyTier()).thenReturn(EmergencyTier.HIGH);

        when(queueEntryRepository.findDetailedById(101L)).thenReturn(Optional.of(entry));
        when(queueEntryRepository.findCanonicalQueueForDoctorAndStatuses(
                eq(21L), eq(QUEUE_DATE), anyCollection()))
                .thenReturn(List.of(entry, second));
    }

    @Test
    void patientCanReadOwnWaitingTimeAndMlPredictionIsUsed() {
        authenticate(UserRole.PATIENT);
        when(patientProfileRepository.findByUserId(44L)).thenReturn(Optional.of(patient));
        when(waitingTimeClient.predict(any())).thenReturn(Optional.of(new WaitingTimePrediction(
                47,
                PredictionSource.ML,
                0.90,
                "waiting-time-rf-v1",
                1,
                "synthetic-demo-data")));

        WaitingTimeResponse response = service.getWaitingTime(101L, 44L);

        assertThat(response.estimatedWaitMinutes()).isEqualTo(47);
        assertThat(response.source()).isEqualTo(PredictionSource.ML);
        assertThat(response.confidence()).isEqualTo(0.90);
        assertThat(response.modelVersion()).isEqualTo("waiting-time-rf-v1");
        assertThat(response.featureSchemaVersion()).isEqualTo(1);
        assertThat(response.trainingDataProvenance()).isEqualTo("synthetic-demo-data");
        assertThat(response.queuePosition()).isEqualTo(1);
        assertThat(response.calculatedAt()).isEqualTo(NOW);
        verifyNoInteractions(doctorProfileRepository);
    }

    @Test
    void patientCannotReadAnotherPatientsWaitingTime() {
        authenticate(UserRole.PATIENT);
        PatientProfile otherPatient = mock(PatientProfile.class);
        when(otherPatient.getId()).thenReturn(99L);
        when(patientProfileRepository.findByUserId(44L)).thenReturn(Optional.of(otherPatient));

        assertThatThrownBy(() -> service.getWaitingTime(101L, 44L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(waitingTimeClient, never()).predict(any());
    }

    @Test
    void doctorCanReadOwnDoctorsWaitingTime() {
        authenticate(UserRole.DOCTOR);
        when(doctorProfileRepository.findByUserId(44L)).thenReturn(Optional.of(doctor));
        when(waitingTimeClient.predict(any())).thenReturn(Optional.empty());

        WaitingTimeResponse response = service.getWaitingTime(101L, 44L);

        assertThat(response.source()).isEqualTo(PredictionSource.FALLBACK);
        assertThat(response.queuePosition()).isEqualTo(1);
        verify(doctorProfileRepository).findByUserId(44L);
        verifyNoInteractions(patientProfileRepository);
    }

    @Test
    void doctorCannotReadAnotherDoctorsWaitingTime() {
        authenticate(UserRole.DOCTOR);
        DoctorProfile otherDoctor = mock(DoctorProfile.class);
        when(otherDoctor.getId()).thenReturn(88L);
        when(doctorProfileRepository.findByUserId(44L)).thenReturn(Optional.of(otherDoctor));

        assertThatThrownBy(() -> service.getWaitingTime(101L, 44L))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(waitingTimeClient, never()).predict(any());
    }

    @Test
    void adminCanReadAnyDoctorsWaitingTime() {
        authenticate(UserRole.ADMIN);
        when(waitingTimeClient.predict(any())).thenReturn(Optional.empty());

        WaitingTimeResponse response = service.getWaitingTime(101L, 44L);

        assertThat(response.source()).isEqualTo(PredictionSource.FALLBACK);
        verifyNoInteractions(patientProfileRepository, doctorProfileRepository);
    }

    @Test
    void clientFailureUsesDeterministicFallback() {
        authenticate(UserRole.PATIENT);
        when(patientProfileRepository.findByUserId(44L)).thenReturn(Optional.of(patient));
        when(waitingTimeClient.predict(any())).thenReturn(Optional.empty());

        WaitingTimeResponse response = service.getWaitingTime(101L, 44L);

        assertThat(response.estimatedWaitMinutes()).isEqualTo(35);
        assertThat(response.source()).isEqualTo(PredictionSource.FALLBACK);
        assertThat(response.confidence()).isEqualTo(0.50);
        assertThat(response.modelVersion()).isNull();
        assertThat(response.featureSchemaVersion()).isNull();
        assertThat(response.trainingDataProvenance()).isNull();
    }

    private void authenticate(UserRole role) {
        when(user.getRole()).thenReturn(role);
        when(userRepository.findById(44L)).thenReturn(Optional.of(user));
    }
}
