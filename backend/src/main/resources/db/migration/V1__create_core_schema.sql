-- All DATETIME(6) values in this schema represent UTC. The application sets
-- both the JDBC session and Hibernate time zone to UTC.
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    role VARCHAR(20) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    phone VARCHAR(32) NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('PATIENT', 'DOCTOR', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE patient_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(32) NULL,
    address VARCHAR(500) NULL,
    emergency_contact VARCHAR(160) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_patient_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_patient_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_departments_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE doctor_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    specialty VARCHAR(120) NOT NULL,
    license_number VARCHAR(64) NOT NULL,
    average_consultation_minutes INT NOT NULL DEFAULT 15,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_doctor_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_doctor_profiles_license UNIQUE (license_number),
    CONSTRAINT fk_doctor_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_doctor_profiles_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE RESTRICT,
    CONSTRAINT ck_doctor_profiles_average_consultation CHECK (average_consultation_minutes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE availability_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME(6) NOT NULL,
    end_time TIME(6) NOT NULL,
    slot_duration_minutes INT NOT NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_availability_templates_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id) ON DELETE CASCADE,
    CONSTRAINT ck_availability_templates_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_availability_templates_time CHECK (start_time < end_time),
    CONSTRAINT ck_availability_templates_duration CHECK (slot_duration_minutes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE schedule_slots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    active BIT(1) NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_schedule_slots_doctor_start UNIQUE (doctor_id, starts_at),
    CONSTRAINT fk_schedule_slots_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT ck_schedule_slots_time CHECK (starts_at < ends_at),
    CONSTRAINT ck_schedule_slots_status CHECK (status IN ('OPEN', 'BOOKED', 'CANCELLED', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE appointments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    slot_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    visit_reason VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    active_marker INT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    cancelled_at DATETIME(6) NULL,
    cancelled_by_user_id BIGINT NULL,
    cancellation_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    -- active_marker=1 consumes the slot for BOOKED/CHECKED_IN appointments.
    -- Terminal and cancelled appointments use NULL, so MySQL permits historical rows
    -- and a slot can be reused without violating uk_appointments_active_slot.
    CONSTRAINT uk_appointments_active_slot UNIQUE (slot_id, active_marker),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patient_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_slot FOREIGN KEY (slot_id) REFERENCES schedule_slots (id) ON DELETE RESTRICT,
    CONSTRAINT fk_appointments_cancelled_by FOREIGN KEY (cancelled_by_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_appointments_status CHECK (status IN ('BOOKED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT ck_appointments_active_marker CHECK (
        (status IN ('BOOKED', 'CHECKED_IN') AND active_marker IS NOT NULL AND active_marker = 1)
        OR (status IN ('COMPLETED', 'CANCELLED', 'NO_SHOW') AND active_marker IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE queue_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    appointment_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    queue_date DATE NOT NULL,
    token_number INT NOT NULL,
    emergency_tier VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    arrived_at DATETIME(6) NOT NULL,
    called_at DATETIME(6) NULL,
    served_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    active_consultation_marker TINYINT GENERATED ALWAYS AS (
        CASE WHEN status = 'IN_CONSULTATION' THEN 1 ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uk_queue_entries_appointment UNIQUE (appointment_id),
    CONSTRAINT uk_queue_entries_token UNIQUE (doctor_id, queue_date, token_number),
    CONSTRAINT uk_queue_entries_active_consultation UNIQUE (doctor_id, active_consultation_marker),
    CONSTRAINT fk_queue_entries_appointment FOREIGN KEY (appointment_id) REFERENCES appointments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_queue_entries_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id) ON DELETE RESTRICT,
    CONSTRAINT ck_queue_entries_token CHECK (token_number > 0),
    CONSTRAINT ck_queue_entries_tier CHECK (emergency_tier IN ('NORMAL', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_queue_entries_status CHECK (status IN ('WAITING', 'CALLED', 'IN_CONSULTATION', 'COMPLETED', 'SKIPPED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE queue_audits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    queue_entry_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    operation VARCHAR(50) NOT NULL,
    previous_value VARCHAR(255) NULL,
    new_value VARCHAR(255) NULL,
    reason VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_queue_audits_entry FOREIGN KEY (queue_entry_id) REFERENCES queue_entries (id) ON DELETE CASCADE,
    CONSTRAINT fk_queue_audits_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_queue_audits_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    family_id VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    replacement_token_hash VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE waiting_time_samples (
    id BIGINT NOT NULL AUTO_INCREMENT,
    doctor_id BIGINT NOT NULL,
    queue_date DATE NOT NULL,
    queue_position INT NOT NULL,
    people_waiting INT NOT NULL,
    average_consultation_minutes INT NOT NULL,
    emergency_count INT NOT NULL,
    triage_tier VARCHAR(20) NOT NULL,
    hour_of_day INT NOT NULL,
    day_of_week INT NOT NULL,
    minutes_from_scheduled_start INT NOT NULL,
    observed_wait_minutes INT NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_waiting_time_samples_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id) ON DELETE CASCADE,
    CONSTRAINT ck_waiting_time_samples_queue_position CHECK (queue_position >= 0),
    CONSTRAINT ck_waiting_time_samples_people_waiting CHECK (people_waiting >= 0),
    CONSTRAINT ck_waiting_time_samples_average_consultation CHECK (average_consultation_minutes > 0),
    CONSTRAINT ck_waiting_time_samples_emergency_count CHECK (emergency_count >= 0),
    CONSTRAINT ck_waiting_time_samples_tier CHECK (triage_tier IN ('NORMAL', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_waiting_time_samples_hour CHECK (hour_of_day BETWEEN 0 AND 23),
    CONSTRAINT ck_waiting_time_samples_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_waiting_time_samples_observed_wait CHECK (observed_wait_minutes >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- uk_users_email, uk_schedule_slots_doctor_start, and uk_refresh_tokens_hash
-- already supply the required unique lookup indexes without duplicate indexes.
CREATE INDEX idx_appointments_patient_date ON appointments (patient_id, appointment_date);
CREATE INDEX idx_appointments_doctor_date ON appointments (doctor_id, appointment_date);
CREATE INDEX idx_queue_entries_doctor_date_status ON queue_entries (doctor_id, queue_date, status);
CREATE INDEX idx_queue_entries_doctor_date_tier_arrival ON queue_entries (doctor_id, queue_date, emergency_tier, arrived_at);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_waiting_time_samples_doctor_date ON waiting_time_samples (doctor_id, queue_date, recorded_at);
