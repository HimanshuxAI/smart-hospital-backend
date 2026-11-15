package com.smarthospital.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.persistence.MySqlIntegrationTest;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.schedule.ScheduleSlotStatus;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.seed.enabled=true")
class DataSeederTest extends MySqlIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private ObjectProvider<PasswordEncoder> passwordEncoderProvider;

    @Test
    @Transactional
    void seedsSyntheticDemoDataWithEncodedPasswordsAndIsIdempotent() throws Exception {
        Map<String, ApplicationRunner> runners = applicationContext.getBeansOfType(ApplicationRunner.class);
        ApplicationRunner seeder = runners.get("dataSeeder");
        assertThat(seeder).as("DataSeeder application runner").isNotNull();

        PasswordEncoder passwordEncoder = passwordEncoderProvider.getIfAvailable();
        assertThat(passwordEncoder).as("PasswordEncoder bean").isNotNull();

        User admin = userRepository.findByEmailIgnoreCase("admin@smarthospital.local").orElseThrow();
        User doctorUser = userRepository.findByEmailIgnoreCase("doctor@smarthospital.local").orElseThrow();
        User patientUser = userRepository.findByEmailIgnoreCase("patient@smarthospital.local").orElseThrow();

        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(doctorUser.getRole()).isEqualTo(UserRole.DOCTOR);
        assertThat(patientUser.getRole()).isEqualTo(UserRole.PATIENT);

        assertThat(admin.getPasswordHash()).isNotEqualTo("Admin@123");
        assertThat(passwordEncoder.matches("Admin@123", admin.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("Doctor@123", doctorUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("Patient@123", patientUser.getPasswordHash())).isTrue();
        assertThat(new BCryptPasswordEncoder().matches("Admin@123", admin.getPasswordHash())).isTrue();

        PatientProfile patient = patientProfileRepository.findByUserId(patientUser.getId()).orElseThrow();
        assertThat(patient.getUser().getId()).isEqualTo(patientUser.getId());

        DoctorProfile doctor = doctorProfileRepository.findByUserId(doctorUser.getId()).orElseThrow();
        assertThat(doctor.getDepartment().getName()).isEqualTo("General Medicine");
        assertThat(doctor.getSpecialty()).isEqualTo("General Medicine");
        assertThat(doctor.getLicenseNumber()).isNotBlank();
        assertThat(doctor.getAverageConsultationMinutes()).isEqualTo(15);

        assertThat(departmentRepository.findByNameIgnoreCase("General Medicine")).isPresent();
        assertThat(departmentRepository.findByNameIgnoreCase("Cardiology")).isPresent();

        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        List<ScheduleSlot> todaySlots = slotsFor(doctor.getId(), todayUtc);
        List<ScheduleSlot> tomorrowSlots = slotsFor(doctor.getId(), todayUtc.plusDays(1));
        assertThat(todaySlots).hasSizeGreaterThanOrEqualTo(2);
        assertThat(tomorrowSlots).hasSizeGreaterThanOrEqualTo(2);
        assertThat(todaySlots).allMatch(slot -> slot.getStatus() == ScheduleSlotStatus.OPEN && slot.isActive());
        assertThat(tomorrowSlots).allMatch(slot -> slot.getStatus() == ScheduleSlotStatus.OPEN && slot.isActive());
        assertThat(tomorrowSlots.get(0).getStartsAt()).isAfter(Instant.now());

        long userCount = userRepository.count();
        long patientProfileCount = patientProfileRepository.count();
        long departmentCount = departmentRepository.count();
        long doctorProfileCount = doctorProfileRepository.count();
        long slotCount = scheduleSlotRepository.count();

        seeder.run(new DefaultApplicationArguments());
        seeder.run(new DefaultApplicationArguments());

        assertThat(userRepository.count()).isEqualTo(userCount);
        assertThat(patientProfileRepository.count()).isEqualTo(patientProfileCount);
        assertThat(departmentRepository.count()).isEqualTo(departmentCount);
        assertThat(doctorProfileRepository.count()).isEqualTo(doctorProfileCount);
        assertThat(scheduleSlotRepository.count()).isEqualTo(slotCount);
    }

    private List<ScheduleSlot> slotsFor(Long doctorId, LocalDate date) {
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return scheduleSlotRepository
                .findByDoctor_IdAndStartsAtBetweenAndStatusAndActiveTrueOrderByStartsAtAsc(
                        doctorId,
                        start,
                        end,
                        ScheduleSlotStatus.OPEN);
    }
}
