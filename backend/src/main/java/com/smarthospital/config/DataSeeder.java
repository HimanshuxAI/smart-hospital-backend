package com.smarthospital.config;

import com.smarthospital.department.Department;
import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String GENERAL_MEDICINE = "General Medicine";
    private static final String CARDIOLOGY = "Cardiology";
    private static final String DEMO_LICENSE_NUMBER = "DEMO-GM-001";
    private static final int CONSULTATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final ZoneId hospitalZoneId;

    public DataSeeder(
            UserRepository userRepository,
            PatientProfileRepository patientProfileRepository,
            DepartmentRepository departmentRepository,
            DoctorProfileRepository doctorProfileRepository,
            ScheduleSlotRepository scheduleSlotRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            Clock clock,
            ZoneId hospitalZoneId) {
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.departmentRepository = departmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.hospitalZoneId = hospitalZoneId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Ensure seed_locks table exists for in-memory or fresh databases
        try {
            jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS seed_locks (id INT PRIMARY KEY)");
            try {
                jdbcTemplate.update("INSERT INTO seed_locks (id) VALUES (1)");
            } catch (Exception ignored) {
                // Row already exists or concurrent insert
            }
            jdbcTemplate.queryForObject(
                    "SELECT id FROM seed_locks WHERE id = 1 FOR UPDATE",
                    Integer.class);
        } catch (Exception ex) {
            log.debug("Proceeding with seeding without lock: {}", ex.getMessage());
        }

        seedUser(
                "admin@smarthospital.local",
                "Demo Administrator",
                UserRole.ADMIN,
                "Admin@123",
                "+1-555-0101");
        User doctorUser = seedUser(
                "doctor@smarthospital.local",
                "Demo Doctor",
                UserRole.DOCTOR,
                "Doctor@123",
                "+1-555-0102");
        User patientUser = seedUser(
                "patient@smarthospital.local",
                "Demo Patient",
                UserRole.PATIENT,
                "Patient@123",
                "+1-555-0103");

        Department generalMedicine = seedDepartment(
                GENERAL_MEDICINE,
                "Primary care and general medical consultations");
        seedDepartment(CARDIOLOGY, "Heart and cardiovascular care");

        if (patientProfileRepository.findByUserId(patientUser.getId()).isEmpty()) {
            PatientProfile patient = new PatientProfile(patientUser, LocalDate.of(1990, 1, 15));
            patient.setGender("Unspecified");
            patient.setAddress("Synthetic Demo Address");
            patient.setEmergencyContact("Demo Emergency Contact, +1-555-0199");
            patientProfileRepository.save(patient);
        }

        DoctorProfile doctor = doctorProfileRepository.findByUserId(doctorUser.getId())
                .orElseGet(() -> doctorProfileRepository.save(new DoctorProfile(
                        doctorUser,
                        generalMedicine,
                        GENERAL_MEDICINE,
                        DEMO_LICENSE_NUMBER,
                        CONSULTATION_MINUTES)));

        LocalDate today = LocalDate.now(clock.withZone(hospitalZoneId));
        seedSlot(doctor, today, LocalTime.of(9, 0));
        seedSlot(doctor, today, LocalTime.of(10, 0));
        seedSlot(doctor, today.plusDays(1), LocalTime.of(9, 0));
        seedSlot(doctor, today.plusDays(1), LocalTime.of(10, 0));
    }

    private User seedUser(String email, String fullName, UserRole role, String password, String phone) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.save(new User(
                        role,
                        fullName,
                        email,
                        passwordEncoder.encode(password),
                        phone)));
    }

    private Department seedDepartment(String name, String description) {
        return departmentRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> departmentRepository.save(new Department(name, description)));
    }

    private void seedSlot(DoctorProfile doctor, LocalDate date, LocalTime startTime) {
        Instant startsAt = date.atTime(startTime).atZone(hospitalZoneId).toInstant();
        if (scheduleSlotRepository.findByDoctor_IdAndStartsAt(doctor.getId(), startsAt).isEmpty()) {
            Instant endsAt = startsAt.plusSeconds(CONSULTATION_MINUTES * 60L);
            scheduleSlotRepository.save(new ScheduleSlot(doctor, startsAt, endsAt));
        }
    }
}
