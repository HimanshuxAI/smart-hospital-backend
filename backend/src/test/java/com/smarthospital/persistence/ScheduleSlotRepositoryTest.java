package com.smarthospital.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.smarthospital.department.Department;
import com.smarthospital.department.DepartmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import com.smarthospital.schedule.ScheduleSlotStatus;
import com.smarthospital.user.User;
import com.smarthospital.user.UserRepository;
import com.smarthospital.user.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ScheduleSlotRepositoryTest extends MySqlIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Test
    void explicitStartRangeExcludesTheExclusiveEndBoundary() {
        DoctorProfile doctor = persistDoctor();
        Instant midnight = Instant.parse("2031-01-02T00:00:00Z");
        ScheduleSlot midnightSlot = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctor,
                midnight,
                midnight.plusSeconds(900)));

        List<ScheduleSlot> priorDate = scheduleSlotRepository.findActiveByDoctorAndStartsAtRange(
                doctor.getId(),
                Instant.parse("2031-01-01T00:00:00Z"),
                midnight,
                ScheduleSlotStatus.OPEN);
        List<ScheduleSlot> nextDate = scheduleSlotRepository.findActiveByDoctorAndStartsAtRange(
                doctor.getId(),
                midnight,
                Instant.parse("2031-01-03T00:00:00Z"),
                ScheduleSlotStatus.OPEN);

        assertThat(priorDate).isEmpty();
        assertThat(nextDate).extracting(ScheduleSlot::getId).containsExactly(midnightSlot.getId());
    }

    @Test
    void activeOverlapUsesHalfOpenIntervals() {
        DoctorProfile doctor = persistDoctor();
        Instant existingStart = Instant.parse("2031-02-01T09:45:00Z");
        Instant existingEnd = Instant.parse("2031-02-01T10:15:00Z");
        ScheduleSlot existing = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctor,
                existingStart,
                existingEnd));

        List<ScheduleSlot> partialOverlap = scheduleSlotRepository.findActiveOverlappingByDoctor(
                doctor.getId(),
                Instant.parse("2031-02-01T10:00:00Z"),
                Instant.parse("2031-02-01T10:30:00Z"));
        List<ScheduleSlot> touchingAfter = scheduleSlotRepository.findActiveOverlappingByDoctor(
                doctor.getId(),
                existingEnd,
                Instant.parse("2031-02-01T11:00:00Z"));
        List<ScheduleSlot> touchingBefore = scheduleSlotRepository.findActiveOverlappingByDoctor(
                doctor.getId(),
                Instant.parse("2031-02-01T08:30:00Z"),
                existingStart);

        assertThat(partialOverlap).extracting(ScheduleSlot::getId).containsExactly(existing.getId());
        assertThat(touchingAfter).isEmpty();
        assertThat(touchingBefore).isEmpty();
    }

    @Test
    void optionalReasonLabelRoundTripsThroughTheEntityMapping() {
        DoctorProfile doctor = persistDoctor();
        Instant startsAt = Instant.parse("2031-03-01T09:00:00Z");
        ScheduleSlot labelled = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctor,
                startsAt,
                startsAt.plusSeconds(900),
                "Elective consultation"));
        ScheduleSlot unlabelled = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctor,
                startsAt.plusSeconds(3600),
                startsAt.plusSeconds(4500)));

        assertThat(labelled.getReasonLabel()).isEqualTo("Elective consultation");
        assertThat(unlabelled.getReasonLabel()).isNull();

        labelled.setReasonLabel("Updated consultation label");
        scheduleSlotRepository.flush();
        assertThat(labelled.getReasonLabel()).isEqualTo("Updated consultation label");
    }

    private DoctorProfile persistDoctor() {
        String unique = UUID.randomUUID().toString();
        User user = userRepository.saveAndFlush(new User(
                UserRole.DOCTOR,
                "Synthetic Doctor",
                "doctor-" + unique + "@example.test",
                "not-used-in-this-test",
                null));
        Department department = departmentRepository.saveAndFlush(new Department(
                "Synthetic-" + unique,
                "Synthetic department"));
        return doctorProfileRepository.saveAndFlush(new DoctorProfile(
                user,
                department,
                "General Medicine",
                "LICENSE-" + unique,
                15));
    }
}
