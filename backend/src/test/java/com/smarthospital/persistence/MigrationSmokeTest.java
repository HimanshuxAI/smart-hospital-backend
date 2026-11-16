package com.smarthospital.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class MigrationSmokeTest extends MySqlIntegrationTest {

    private static final List<String> REQUIRED_TABLES = List.of(
            "users",
            "patient_profiles",
            "departments",
            "doctor_profiles",
            "availability_templates",
            "schedule_slots",
            "appointments",
            "queue_entries",
            "queue_audits",
            "refresh_tokens",
            "waiting_time_samples",
            "seed_locks");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesCoreSchemaAndAllRequiredTablesExist() {
        assertThat(tableExists("flyway_schema_history"))
                .as("Flyway schema history table")
                .isTrue();

        Integer successfulVersionOneMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1",
                Integer.class);
        assertThat(successfulVersionOneMigrations).isEqualTo(1);

        Integer successfulVersionTwoMigrations = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2' AND success = 1",
                Integer.class);
        assertThat(successfulVersionTwoMigrations).isEqualTo(1);

        for (String table : REQUIRED_TABLES) {
            assertThat(tableExists(table)).as("required table %s", table).isTrue();
        }
    }

    @Test
    void forwardMigrationAddsPersistenceHardeningColumnsAndRetainsAuditHistory() {
        assertThat(columnExists("schedule_slots", "reason_label")).isTrue();
        assertThat(columnExists("appointments", "checked_in_at")).isTrue();
        assertThat(columnExists("appointments", "completed_at")).isTrue();
        assertThat(columnExists("appointments", "no_show_at")).isTrue();

        String deleteRule = jdbcTemplate.queryForObject(
                "SELECT delete_rule FROM information_schema.referential_constraints "
                        + "WHERE constraint_schema = DATABASE() "
                        + "AND constraint_name = 'fk_queue_audits_entry'",
                String.class);
        assertThat(deleteRule).isEqualTo("RESTRICT");

        Integer seedLockRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM seed_locks WHERE id = 1",
                Integer.class);
        assertThat(seedLockRows).isEqualTo(1);
    }

    @Test
    void schemaDefinesRequiredUniquenessAndLookupIndexes() {
        List<String> uniqueConstraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints "
                        + "WHERE constraint_schema = DATABASE() AND constraint_type = 'UNIQUE'",
                String.class);
        assertThat(uniqueConstraints).contains(
                "uk_users_email",
                "uk_patient_profiles_user",
                "uk_doctor_profiles_user",
                "uk_doctor_profiles_license",
                "uk_schedule_slots_doctor_start",
                "uk_appointments_active_slot",
                "uk_queue_entries_appointment",
                "uk_queue_entries_token",
                "uk_queue_entries_active_consultation",
                "uk_refresh_tokens_hash");

        Set<String> indexes = Set.copyOf(jdbcTemplate.queryForList(
                "SELECT DISTINCT index_name FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE()",
                String.class));
        assertThat(indexes).contains(
                "uk_users_email",
                "uk_schedule_slots_doctor_start",
                "uk_refresh_tokens_hash",
                "idx_appointments_patient_date",
                "idx_appointments_doctor_date",
                "idx_queue_entries_doctor_date_status",
                "idx_refresh_tokens_family");
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() "
                        + "AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName);
        return count != null && count == 1;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count == 1;
    }
}
