package com.smarthospital.appointment;

import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.user.User;
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
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "appointments", uniqueConstraints = @UniqueConstraint(
        name = "uk_appointments_active_slot", columnNames = {"slot_id", "active_marker"}))
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private DoctorProfile doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_id", nullable = false)
    private ScheduleSlot slot;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "visit_reason", nullable = false, length = 500)
    private String visitReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppointmentStatus status;

    @Column(name = "active_marker")
    private Integer activeMarker;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "no_show_at")
    private Instant noShowAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    private User cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Appointment() {
    }

    public Appointment(
            PatientProfile patient,
            DoctorProfile doctor,
            ScheduleSlot slot,
            LocalDate appointmentDate,
            String visitReason) {
        this.patient = Objects.requireNonNull(patient, "patient must not be null");
        this.doctor = Objects.requireNonNull(doctor, "doctor must not be null");
        this.slot = Objects.requireNonNull(slot, "slot must not be null");
        this.appointmentDate = Objects.requireNonNull(appointmentDate, "appointmentDate must not be null");
        this.visitReason = Objects.requireNonNull(visitReason, "visitReason must not be null");
        this.status = AppointmentStatus.BOOKED;
        this.activeMarker = 1;
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

    public void checkIn(Instant checkedInAt) {
        requireStatus(AppointmentStatus.BOOKED, "check in");
        Instant validatedCheckedInAt = Objects.requireNonNull(checkedInAt, "checkedInAt must not be null");
        this.checkedInAt = validatedCheckedInAt;
        this.status = AppointmentStatus.CHECKED_IN;
        this.activeMarker = 1;
    }

    public void cancel(User actor, String reason, Instant cancelledAt) {
        requireStatus(AppointmentStatus.BOOKED, "cancel");
        User validatedActor = Objects.requireNonNull(actor, "actor must not be null");
        String validatedReason = Objects.requireNonNull(reason, "reason must not be null");
        if (validatedReason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        Instant validatedCancelledAt = Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        this.cancelledBy = validatedActor;
        this.cancellationReason = validatedReason;
        this.cancelledAt = validatedCancelledAt;
        this.status = AppointmentStatus.CANCELLED;
        this.activeMarker = null;
    }

    public void markNoShow(Instant noShowAt) {
        requireStatus(AppointmentStatus.BOOKED, "mark no-show");
        this.noShowAt = Objects.requireNonNull(noShowAt, "noShowAt must not be null");
        this.status = AppointmentStatus.NO_SHOW;
        this.activeMarker = null;
    }

    public void complete(Instant completedAt) {
        requireStatus(AppointmentStatus.CHECKED_IN, "complete");
        this.completedAt = Objects.requireNonNull(completedAt, "completedAt must not be null");
        this.status = AppointmentStatus.COMPLETED;
        this.activeMarker = null;
    }

    private void requireStatus(AppointmentStatus requiredStatus, String operation) {
        if (status != requiredStatus) {
            throw new IllegalStateException(
                    "cannot " + operation + " appointment in status " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public PatientProfile getPatient() {
        return patient;
    }

    public DoctorProfile getDoctor() {
        return doctor;
    }

    public ScheduleSlot getSlot() {
        return slot;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public String getVisitReason() {
        return visitReason;
    }

    public void setVisitReason(String visitReason) {
        this.visitReason = Objects.requireNonNull(visitReason, "visitReason must not be null");
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public Integer getActiveMarker() {
        return activeMarker;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getNoShowAt() {
        return noShowAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public User getCancelledBy() {
        return cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
