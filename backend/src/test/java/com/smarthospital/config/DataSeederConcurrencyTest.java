package com.smarthospital.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.patient.PatientProfileRepository;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest(properties = {
        "app.seed.enabled=true",
        "smart-hospital.time-zone=UTC"
})
class DataSeederConcurrencyTest {

    private static final int CONCURRENT_SEEDERS = 4;
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("smarthospital_seed_concurrency")
            .withUsername("smarthospital")
            .withPassword("smarthospital-test-password");

    static {
        MYSQL.start();
    }

    @Autowired
    private DataSeeder dataSeeder;

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
    private JdbcTemplate jdbcTemplate;

    @TestBean(name = "clock", methodName = "fixedClock")
    private Clock clock;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> MYSQL.getJdbcUrl()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.security.user.name", () -> "integration-test-user");
        registry.add("spring.security.user.password", () -> "integration-test-password");
        registry.add("smart-hospital.security.jwt.secret",
                () -> "integration-test-jwt-secret-with-at-least-32-characters");
        registry.add("smart-hospital.security.jwt.access-token-expiration", () -> "PT15M");
        registry.add("smart-hospital.security.refresh-token.expiration", () -> "P7D");
        registry.add("spring.main.banner-mode", () -> "off");
    }

    @BeforeEach
    void removeStartupSeedData() {
        jdbcTemplate.update("""
                DELETE FROM schedule_slots
                WHERE doctor_id IN (
                    SELECT dp.id FROM doctor_profiles dp
                    JOIN users u ON u.id = dp.user_id
                    WHERE u.email IN ('admin@smarthospital.local',
                                      'doctor@smarthospital.local',
                                      'patient@smarthospital.local')
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM doctor_profiles
                WHERE user_id IN (
                    SELECT id FROM users
                    WHERE email IN ('admin@smarthospital.local',
                                   'doctor@smarthospital.local',
                                   'patient@smarthospital.local')
                )
                """);
        jdbcTemplate.update("""
                DELETE FROM patient_profiles
                WHERE user_id IN (
                    SELECT id FROM users
                    WHERE email IN ('admin@smarthospital.local',
                                   'doctor@smarthospital.local',
                                   'patient@smarthospital.local')
                )
                """);
        jdbcTemplate.update("DELETE FROM departments WHERE name IN ('General Medicine', 'Cardiology')");
        jdbcTemplate.update("""
                DELETE FROM users
                WHERE email IN ('admin@smarthospital.local',
                                'doctor@smarthospital.local',
                                'patient@smarthospital.local')
                """);

        assertThat(userRepository.count()).isZero();
        assertThat(patientProfileRepository.count()).isZero();
        assertThat(departmentRepository.count()).isZero();
        assertThat(doctorProfileRepository.count()).isZero();
        assertThat(scheduleSlotRepository.count()).isZero();
    }

    @Test
    void concurrentSeedersCreateOneConsistentDatasetAndNeverOverwritePasswordHashes() throws Exception {
        runConcurrentSeeders();

        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(patientProfileRepository.count()).isEqualTo(1);
        assertThat(departmentRepository.count()).isEqualTo(2);
        assertThat(doctorProfileRepository.count()).isEqualTo(1);
        assertThat(scheduleSlotRepository.count()).isEqualTo(4);

        Map<String, String> originalHashes = passwordHashesByEmail();
        assertThat(originalHashes).hasSize(3).allSatisfy((email, hash) -> assertThat(hash).isNotBlank());

        runConcurrentSeeders();

        assertThat(passwordHashesByEmail()).isEqualTo(originalHashes);
        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(patientProfileRepository.count()).isEqualTo(1);
        assertThat(departmentRepository.count()).isEqualTo(2);
        assertThat(doctorProfileRepository.count()).isEqualTo(1);
        assertThat(scheduleSlotRepository.count()).isEqualTo(4);
    }

    private void runConcurrentSeeders() throws Exception {
        CyclicBarrier startBarrier = new CyclicBarrier(CONCURRENT_SEEDERS);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_SEEDERS);
        List<Future<Void>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < CONCURRENT_SEEDERS; index++) {
                futures.add(executor.submit(() -> {
                    startBarrier.await(5, TimeUnit.SECONDS);
                    dataSeeder.run(new DefaultApplicationArguments());
                    return null;
                }));
            }

            for (Future<Void> future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private Map<String, String> passwordHashesByEmail() {
        return userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getEmail, User::getPasswordHash));
    }

    static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2030-01-15T00:00:00Z"), ZoneOffset.UTC);
    }
}
