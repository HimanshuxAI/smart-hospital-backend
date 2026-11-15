package com.smarthospital.doctor;

import com.smarthospital.department.Department;
import com.smarthospital.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "doctor_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_doctor_profiles_user", columnNames = "user_id"),
        @UniqueConstraint(name = "uk_doctor_profiles_license", columnNames = "license_number")
})
public class DoctorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "specialty", nullable = false, length = 120)
    private String specialty;

    @Column(name = "license_number", nullable = false, length = 64)
    private String licenseNumber;

    @Column(name = "average_consultation_minutes", nullable = false)
    private int averageConsultationMinutes;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DoctorProfile() {
    }

    public DoctorProfile(
            User user,
            Department department,
            String specialty,
            String licenseNumber,
            int averageConsultationMinutes) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.department = Objects.requireNonNull(department, "department must not be null");
        this.specialty = Objects.requireNonNull(specialty, "specialty must not be null");
        this.licenseNumber = Objects.requireNonNull(licenseNumber, "licenseNumber must not be null");
        this.averageConsultationMinutes = averageConsultationMinutes;
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

    public User getUser() {
        return user;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = Objects.requireNonNull(department, "department must not be null");
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = Objects.requireNonNull(specialty, "specialty must not be null");
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = Objects.requireNonNull(licenseNumber, "licenseNumber must not be null");
    }

    public int getAverageConsultationMinutes() {
        return averageConsultationMinutes;
    }

    public void setAverageConsultationMinutes(int averageConsultationMinutes) {
        this.averageConsultationMinutes = averageConsultationMinutes;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
