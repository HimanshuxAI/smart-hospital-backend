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

    List<QueueEntry> findByDoctor_IdAndQueueDateOrderByArrivedAtAsc(Long doctorId, LocalDate queueDate);

    List<QueueEntry> findByDoctor_IdAndQueueDateAndStatusOrderByEmergencyTierAscArrivedAtAsc(
            Long doctorId,
            LocalDate queueDate,
            QueueStatus status);

    List<QueueEntry> findByDoctor_IdAndQueueDateAndStatusInOrderByEmergencyTierAscArrivedAtAsc(
            Long doctorId,
            LocalDate queueDate,
            Collection<QueueStatus> statuses);

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
    @Query("select q from QueueEntry q "
            + "join fetch q.doctor "
            + "join fetch q.appointment "
            + "where q.doctor.id = :doctorId "
            + "and q.queueDate = :queueDate "
            + "and q.status in :statuses "
            + "order by q.emergencyTier asc, q.arrivedAt asc")
    List<QueueEntry> findNextCandidatesForUpdate(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate,
            @Param("statuses") Collection<QueueStatus> statuses);
}
