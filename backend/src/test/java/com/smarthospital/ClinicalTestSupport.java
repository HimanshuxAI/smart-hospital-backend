package com.smarthospital;

import com.smarthospital.department.Department;
import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfile;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.persistence.MySqlIntegrationTest;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;

@Import(ClinicalTestSupport.FixedClinicalClockConfiguration.class)
public abstract class ClinicalTestSupport extends MySqlIntegrationTest {

    protected static final Instant NOW = Instant.parse("2030-01-15T08:00:00Z");
    protected static final LocalDate TODAY = LocalDate.of(2030, 1, 15);

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PatientProfileRepository patientProfileRepository;

    @Autowired
    protected DepartmentRepository departmentRepository;

    @Autowired
    protected DoctorProfileRepository doctorProfileRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected com.smarthospital.auth.JwtService jwtService;

    protected TestPatient createPatient() {
        User user = userRepository.saveAndFlush(new User(
                UserRole.PATIENT,
                "Clinical Test Patient",
                uniqueEmail("patient"),
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
        PatientProfile profile = patientProfileRepository.saveAndFlush(new PatientProfile(
                user,
                LocalDate.of(1990, 1, 15)));
        return new TestPatient(
                user.getId(),
                profile.getId(),
                user.getEmail(),
                jwtService.issueAccessToken(user));
    }

    protected TestDoctor createDoctor() {
        Department department = createDepartment();
        return createDoctor(department);
    }

    protected TestDoctor createDoctor(Department department) {
        User user = userRepository.saveAndFlush(new User(
                UserRole.DOCTOR,
                "Clinical Test Doctor",
                uniqueEmail("doctor"),
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
        DoctorProfile profile = doctorProfileRepository.saveAndFlush(new DoctorProfile(
                user,
                department,
                "General Medicine",
                "LIC-" + UUID.randomUUID(),
                15));
        return new TestDoctor(
                user.getId(),
                profile.getId(),
                department.getId(),
                user.getEmail(),
                jwtService.issueAccessToken(user));
    }

    protected TestAdmin createAdmin() {
        User user = userRepository.saveAndFlush(new User(
                UserRole.ADMIN,
                "Clinical Test Admin",
                uniqueEmail("admin"),
                passwordEncoder.encode("StrongPassword@123"),
                "+1-555-0100"));
        return new TestAdmin(user.getId(), jwtService.issueAccessToken(user));
    }

    protected Department createDepartment() {
        return departmentRepository.saveAndFlush(new Department(
                "Clinical Department " + UUID.randomUUID(),
                "Clinical test department"));
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }

    protected record TestPatient(Long userId, Long patientId, String email, String accessToken) {
    }

    protected record TestDoctor(
            Long userId,
            Long doctorId,
            Long departmentId,
            String email,
            String accessToken) {
    }

    protected record TestAdmin(Long userId, String accessToken) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClinicalClockConfiguration {

        @Bean
        @Primary
        Clock clinicalClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
