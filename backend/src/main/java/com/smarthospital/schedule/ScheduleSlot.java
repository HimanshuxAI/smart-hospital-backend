package com.smarthospital.schedule;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "schedule_slots", uniqueConstraints = @UniqueConstraint(
        name = "uk_schedule_slots_doctor_start", columnNames = {"doctor_id", "starts_at"}))
public class ScheduleSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleSlotStatus status;

    @Column(name = "reason_label", length = 255)
    private String reasonLabel;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ScheduleSlot() {
    }

    public ScheduleSlot(DoctorProfile doctor, Instant startsAt, Instant endsAt) {
        this(doctor, startsAt, endsAt, null);
    }

    public ScheduleSlot(DoctorProfile doctor, Instant startsAt, Instant endsAt, String reasonLabel) {
        this.doctor = Objects.requireNonNull(doctor, "doctor must not be null");
        this.startsAt = Objects.requireNonNull(startsAt, "startsAt must not be null");
        this.endsAt = Objects.requireNonNull(endsAt, "endsAt must not be null");
        this.reasonLabel = reasonLabel;
        this.status = ScheduleSlotStatus.OPEN;
        this.active = true;
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

    public Long getId() {
        return id;
    }

    public DoctorProfile getDoctor() {
        return doctor;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public ScheduleSlotStatus getStatus() {
        return status;
    }

    public String getReasonLabel() {
        return reasonLabel;
    }

    public void setReasonLabel(String reasonLabel) {
        this.reasonLabel = reasonLabel;
    }

    public void markBooked() {
        requireStatus(ScheduleSlotStatus.OPEN, "book");
        status = ScheduleSlotStatus.BOOKED;
        active = true;
    }

    /**
     * Releases a booked slot so it can be booked again. The Task 5 cancellation flow
     * must lock and cancel the appointment before locking and releasing the slot, and
     * must perform both operations in the same transaction.
     */
    public void release() {
        requireStatus(ScheduleSlotStatus.BOOKED, "release");
        status = ScheduleSlotStatus.OPEN;
        active = true;
    }

    public void cancel() {
        if (status != ScheduleSlotStatus.OPEN && status != ScheduleSlotStatus.BOOKED) {
            throw new IllegalStateException("cannot cancel schedule slot in status " + status);
        }
        status = ScheduleSlotStatus.CANCELLED;
        active = false;
    }

    public void complete() {
        requireStatus(ScheduleSlotStatus.BOOKED, "complete");
        status = ScheduleSlotStatus.COMPLETED;
        active = false;
    }

    private void requireStatus(ScheduleSlotStatus requiredStatus, String operation) {
        if (status != requiredStatus) {
            throw new IllegalStateException(
                    "cannot " + operation + " schedule slot in status " + status);
        }
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
