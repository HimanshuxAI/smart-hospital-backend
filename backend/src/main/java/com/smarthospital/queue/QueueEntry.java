package com.smarthospital.queue;

import com.smarthospital.appointment.Appointment;
import com.smarthospital.doctor.DoctorProfile;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "queue_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_queue_entries_appointment", columnNames = "appointment_id"),
        @UniqueConstraint(
                name = "uk_queue_entries_token",
                columnNames = {"doctor_id", "queue_date", "token_number"})
})
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @Column(name = "queue_date", nullable = false)
    private LocalDate queueDate;

    @Column(name = "token_number", nullable = false)
    private int tokenNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "emergency_tier", nullable = false, length = 20)
    private EmergencyTier emergencyTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private QueueStatus status;

    @Column(name = "arrived_at", nullable = false)
    private Instant arrivedAt;

    @Column(name = "called_at")
    private Instant calledAt;

    @Column(name = "served_at")
    private Instant servedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected QueueEntry() {
    }

    public QueueEntry(
            Appointment appointment,
            DoctorProfile doctor,
            LocalDate queueDate,
            int tokenNumber,
            EmergencyTier emergencyTier,
            Instant arrivedAt) {
        this.appointment = Objects.requireNonNull(appointment, "appointment must not be null");
        this.doctor = Objects.requireNonNull(doctor, "doctor must not be null");
        this.queueDate = Objects.requireNonNull(queueDate, "queueDate must not be null");
        this.tokenNumber = tokenNumber;
        this.emergencyTier = Objects.requireNonNull(emergencyTier, "emergencyTier must not be null");
        this.arrivedAt = Objects.requireNonNull(arrivedAt, "arrivedAt must not be null");
        this.status = QueueStatus.WAITING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void call(Instant calledAt) {
        requireStatus(QueueStatus.WAITING, "call");
        this.calledAt = Objects.requireNonNull(calledAt, "calledAt must not be null");
        this.status = QueueStatus.CALLED;
    }

    public void startConsultation(Instant servedAt) {
        requireStatus(QueueStatus.CALLED, "start consultation");
        this.servedAt = Objects.requireNonNull(servedAt, "servedAt must not be null");
        this.status = QueueStatus.IN_CONSULTATION;
    }

    public void complete(Instant completedAt) {
        requireStatus(QueueStatus.IN_CONSULTATION, "complete");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        this.status = QueueStatus.COMPLETED;
    }

    public void skip() {
        requireStatus(QueueStatus.WAITING, "skip");
        this.status = QueueStatus.SKIPPED;
    }

    public void returnToWaiting() {
        requireStatus(QueueStatus.SKIPPED, "return to waiting");
        this.status = QueueStatus.WAITING;
    }

    public void cancel() {
        if (status != QueueStatus.WAITING && status != QueueStatus.CALLED) {
            throw new IllegalStateException("cannot cancel queue entry in status " + status);
        }
        this.status = QueueStatus.CANCELLED;
    }

    private void requireStatus(QueueStatus requiredStatus, String operation) {
        if (status != requiredStatus) {
            throw new IllegalStateException(
                    "cannot " + operation + " queue entry in status " + status);
        }
    }

    public void classify(EmergencyTier newTier) {
        emergencyTier = Objects.requireNonNull(newTier, "newTier must not be null");
    }

    public Long getId() {
        return id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public DoctorProfile getDoctor() {
        return doctor;
    }

    public LocalDate getQueueDate() {
        return queueDate;
    }

    public int getTokenNumber() {
        return tokenNumber;
    }

    public EmergencyTier getEmergencyTier() {
        return emergencyTier;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public Instant getArrivedAt() {
        return arrivedAt;
    }

    public Instant getCalledAt() {
        return calledAt;
    }

    public Instant getServedAt() {
        return servedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
