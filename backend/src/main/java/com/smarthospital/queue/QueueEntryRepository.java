package com.smarthospital.queue;

import com.smarthospital.dashboard.DashboardQueueProjection;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByAppointment_Id(Long appointmentId);

    @Query("""
            select q from QueueEntry q
            join fetch q.appointment a
            join fetch a.patient p
            join fetch a.slot s
            join fetch q.doctor d
            where q.id = :entryId
            """)
    Optional<QueueEntry> findDetailedById(@Param("entryId") Long entryId);

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

    @Query("""
            select q.id as entryId,
                   a.id as appointmentId,
                   doctorUser.fullName as doctorName,
                   patientUser.fullName as patientName,
                   q.queueDate as queueDate,
                   q.tokenNumber as tokenNumber,
                   q.emergencyTier as emergencyTier,
                   q.status as status,
                   q.arrivedAt as arrivedAt,
                   s.startsAt as scheduledStart
            from QueueEntry q
            join q.appointment a
            join a.patient p
            join p.user patientUser
            join a.slot s
            join q.doctor d
            join d.user doctorUser
            where a.patient.id = :patientId
              and q.queueDate = :queueDate
              and q.status in (
                  com.smarthospital.queue.QueueStatus.WAITING,
                  com.smarthospital.queue.QueueStatus.CALLED,
                  com.smarthospital.queue.QueueStatus.IN_CONSULTATION)
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
    List<DashboardQueueProjection> findFirstActiveForPatient(
            @Param("patientId") Long patientId,
            @Param("queueDate") LocalDate queueDate,
            Pageable pageable);

    @Query("""
            select q.id as entryId,
                   a.id as appointmentId,
                   doctorUser.fullName as doctorName,
                   patientUser.fullName as patientName,
                   q.queueDate as queueDate,
                   q.tokenNumber as tokenNumber,
                   q.emergencyTier as emergencyTier,
                   q.status as status,
                   q.arrivedAt as arrivedAt,
                   s.startsAt as scheduledStart
            from QueueEntry q
            join q.appointment a
            join a.patient p
            join p.user patientUser
            join a.slot s
            join q.doctor d
            join d.user doctorUser
            where q.doctor.id = :doctorId
              and q.queueDate = :queueDate
              and q.status in (
                  com.smarthospital.queue.QueueStatus.WAITING,
                  com.smarthospital.queue.QueueStatus.CALLED,
                  com.smarthospital.queue.QueueStatus.IN_CONSULTATION)
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
    List<DashboardQueueProjection> findCurrentQueueForDoctor(
            @Param("doctorId") Long doctorId,
            @Param("queueDate") LocalDate queueDate);

    long countByQueueDate(LocalDate queueDate);

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
