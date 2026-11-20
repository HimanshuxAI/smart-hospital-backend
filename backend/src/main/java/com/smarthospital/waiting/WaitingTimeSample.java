package com.smarthospital.waiting;

import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.queue.EmergencyTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "waiting_time_samples")
public class WaitingTimeSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "queue_position", nullable = false)
    private int queuePosition;

    @Column(name = "people_waiting", nullable = false)
    private int peopleWaiting;

    @Column(name = "average_consultation_minutes", nullable = false)
    private int averageConsultationMinutes;

    @Column(name = "emergency_count", nullable = false)
    private int emergencyCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "triage_tier", nullable = false, length = 20)
    private EmergencyTier triageTier;

    @Column(name = "hour_of_day", nullable = false)
    private int hourOfDay;

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "minutes_from_scheduled_start", nullable = false)
    private int minutesFromScheduledStart;

    @Column(name = "observed_wait_minutes", nullable = false)
    private int observedWaitMinutes;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected WaitingTimeSample() {
    }

    public WaitingTimeSample(
            DoctorProfile doctor,
            LocalDate queueDate,
            int queuePosition,
            int peopleWaiting,
            int averageConsultationMinutes,
            int emergencyCount,
            EmergencyTier triageTier,
            int hourOfDay,
            int dayOfWeek,
            int minutesFromScheduledStart,
            int observedWaitMinutes,
            Instant recordedAt) {
        this.doctor = Objects.requireNonNull(doctor, "doctor must not be null");
        this.queueDate = Objects.requireNonNull(queueDate, "queueDate must not be null");
        this.queuePosition = queuePosition;
        this.peopleWaiting = peopleWaiting;
        this.averageConsultationMinutes = averageConsultationMinutes;
        this.emergencyCount = emergencyCount;
        this.triageTier = Objects.requireNonNull(triageTier, "triageTier must not be null");
        this.hourOfDay = hourOfDay;
        this.dayOfWeek = dayOfWeek;
        this.minutesFromScheduledStart = minutesFromScheduledStart;
        this.observedWaitMinutes = observedWaitMinutes;
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    }

    public Long getId() {
        return id;
    }

    public DoctorProfile getDoctor() {
        return doctor;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public int getPeopleWaiting() {
        return peopleWaiting;
    }

    public int getAverageConsultationMinutes() {
        return averageConsultationMinutes;
    }

    public int getEmergencyCount() {
        return emergencyCount;
    }

    public EmergencyTier getTriageTier() {
        return triageTier;
    }

    public int getHourOfDay() {
        return hourOfDay;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public int getMinutesFromScheduledStart() {
        return minutesFromScheduledStart;
    }

    public int getObservedWaitMinutes() {
        return observedWaitMinutes;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
