package com.smarthospital.queue;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByAppointment_Id(Long appointmentId);

    Optional<QueueEntry> findByDoctor_IdAndQueueDateAndTokenNumber(
            Long doctorId,
            LocalDate queueDate,
            int tokenNumber);

    @Query("""
            select q from QueueEntry q
            where q.doctor.id = :doctorId
              and q.queueDate = :queueDate
            order by
              case
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.CRITICAL then 0
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.HIGH then 1
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.MEDIUM then 2
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.NORMAL then 3
                else 4
              end,
              q.arrivedAt asc,
              q.id asc
            """)
    List<QueueEntry> findCanonicalQueueForDoctor(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate);

    @Query("""
            select q from QueueEntry q
            where q.doctor.id = :doctorId
              and q.queueDate = :queueDate
              and q.status = :status
            order by
              case
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.CRITICAL then 0
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.HIGH then 1
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.MEDIUM then 2
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.NORMAL then 3
                else 4
              end,
              q.arrivedAt asc,
              q.id asc
            """)
    List<QueueEntry> findCanonicalQueueForDoctorAndStatus(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate,
            @Param("status") QueueStatus status);

    @Query("""
            select q from QueueEntry q
            where q.doctor.id = :doctorId
              and q.queueDate = :queueDate
              and q.status in :statuses
            order by
              case
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.CRITICAL then 0
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.HIGH then 1
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.MEDIUM then 2
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.NORMAL then 3
                else 4
              end,
              q.arrivedAt asc,
              q.id asc
            """)
    List<QueueEntry> findCanonicalQueueForDoctorAndStatuses(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate,
            @Param("statuses") Collection<QueueStatus> statuses);

    @Query("select coalesce(max(q.tokenNumber), 0) from QueueEntry q "
            + "where q.doctor.id = :doctorId and q.queueDate = :queueDate")
    int findMaximumTokenNumber(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from QueueEntry q "
            + "join fetch q.doctor "
            + "join fetch q.appointment "
            + "where q.id = :entryId")
    Optional<QueueEntry> findByIdForUpdate(@Param("entryId") Long entryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select q from QueueEntry q
            join fetch q.doctor
            join fetch q.appointment
            where q.doctor.id = :doctorId
              and q.queueDate = :queueDate
              and q.status in :statuses
            order by
              case
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.CRITICAL then 0
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.HIGH then 1
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.MEDIUM then 2
                when q.emergencyTier = com.smarthospital.queue.EmergencyTier.NORMAL then 3
                else 4
              end,
              q.arrivedAt asc,
              q.id asc
            """)
    List<QueueEntry> findNextCandidatesForUpdate(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate,
            @Param("statuses") Collection<QueueStatus> statuses);
}
