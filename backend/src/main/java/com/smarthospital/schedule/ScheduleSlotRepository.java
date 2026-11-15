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

    List<ScheduleSlot> findByDoctor_IdAndStartsAtBetweenAndStatusAndActiveTrueOrderByStartsAtAsc(
            Long doctorId,
            Instant startsAtInclusive,
            Instant endsAtExclusive,
            ScheduleSlotStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScheduleSlot s join fetch s.doctor where s.id = :slotId")
    Optional<ScheduleSlot> findByIdForUpdate(@Param("slotId") Long slotId);
}
