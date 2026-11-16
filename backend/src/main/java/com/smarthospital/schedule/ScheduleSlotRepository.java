package com.smarthospital.schedule;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    Optional<ScheduleSlot> findByDoctor_IdAndStartsAt(Long doctorId, Instant startsAt);

    @Query("""
            select s from ScheduleSlot s
            where s.doctor.id = :doctorId
              and s.startsAt >= :rangeStart
              and s.startsAt < :rangeEnd
              and s.status = :status
              and s.active = true
            order by s.startsAt asc
            """)
    List<ScheduleSlot> findActiveByDoctorAndStartsAtRange(
            @Param("doctorId") Long doctorId,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            @Param("status") ScheduleSlotStatus status);

    @Query("""
            select s from ScheduleSlot s
            where s.doctor.id = :doctorId
              and s.active = true
              and s.status in (
                  com.smarthospital.schedule.ScheduleSlotStatus.OPEN,
                  com.smarthospital.schedule.ScheduleSlotStatus.BOOKED
              )
              and s.startsAt < :proposedEnd
              and s.endsAt > :proposedStart
            order by s.startsAt asc, s.id asc
            """)
    List<ScheduleSlot> findActiveOverlappingByDoctor(
            @Param("doctorId") Long doctorId,
            @Param("proposedStart") Instant proposedStart,
            @Param("proposedEnd") Instant proposedEnd);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScheduleSlot s join fetch s.doctor where s.id = :slotId")
    Optional<ScheduleSlot> findByIdForUpdate(@Param("slotId") Long slotId);
}
