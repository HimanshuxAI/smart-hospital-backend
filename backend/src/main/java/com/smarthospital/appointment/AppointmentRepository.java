package com.smarthospital.appointment;

import com.smarthospital.dashboard.DashboardAppointmentProjection;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findBySlot_IdAndActiveMarker(Long slotId, Integer activeMarker);

    List<Appointment> findByPatient_IdAndAppointmentDateOrderByIdAsc(
            Long patientId,
            LocalDate appointmentDate);

    List<Appointment> findByPatient_IdAndAppointmentDateBetweenOrderByAppointmentDateAscIdAsc(
            Long patientId,
            LocalDate startsAt,
            LocalDate endsAt);

    List<Appointment> findByDoctor_IdAndAppointmentDateOrderByIdAsc(Long doctorId, LocalDate appointmentDate);

    List<Appointment> findByPatient_IdOrderByAppointmentDateAscIdAsc(Long patientId);

    List<Appointment> findByDoctor_IdOrderByAppointmentDateAscIdAsc(Long doctorId);

    List<Appointment> findAllByOrderByAppointmentDateAscIdAsc();

    List<Appointment> findByDoctor_IdAndAppointmentDateBetweenOrderByAppointmentDateAscIdAsc(
            Long doctorId,
            LocalDate startsAt,
            LocalDate endsAt);

    @Query("""
            select a from Appointment a
            join a.slot s
            where a.patient.id = :patientId
              and a.status in (
                  com.smarthospital.appointment.AppointmentStatus.BOOKED,
                  com.smarthospital.appointment.AppointmentStatus.CHECKED_IN)
              and s.startsAt < :proposedEnd
              and s.endsAt > :proposedStart
            order by s.startsAt asc, a.id asc
            """)
    List<Appointment> findActiveOverlappingForPatient(
            @Param("patientId") Long patientId,
            @Param("proposedStart") Instant proposedStart,
            @Param("proposedEnd") Instant proposedEnd);

    @Query("select a.doctor.id from Appointment a where a.id = :appointmentId")
    Optional<Long> findDoctorIdById(@Param("appointmentId") Long appointmentId);

    @Query("select a.appointmentDate from Appointment a where a.id = :appointmentId")
    Optional<LocalDate> findAppointmentDateById(@Param("appointmentId") Long appointmentId);

    @Query("""
            select a.id as appointmentId,
                   doctorUser.fullName as doctorName,
                   d.specialty as specialty,
                   patientUser.fullName as patientName,
                   s.startsAt as startsAt,
                   s.endsAt as endsAt,
                   a.status as status,
                   a.visitReason as visitReason
            from Appointment a
            join a.doctor d
            join d.user doctorUser
            join a.patient p
            join p.user patientUser
            join a.slot s
            where a.patient.id = :patientId
              and a.appointmentDate >= :fromDate
              and a.status in (
                  com.smarthospital.appointment.AppointmentStatus.BOOKED,
                  com.smarthospital.appointment.AppointmentStatus.CHECKED_IN)
            order by s.startsAt asc, a.id asc
            """)
    List<DashboardAppointmentProjection> findUpcomingForPatient(
            @Param("patientId") Long patientId,
            @Param("fromDate") LocalDate fromDate);

    @Query("""
            select a.id as appointmentId,
                   doctorUser.fullName as doctorName,
                   d.specialty as specialty,
                   patientUser.fullName as patientName,
                   s.startsAt as startsAt,
                   s.endsAt as endsAt,
                   a.status as status,
                   a.visitReason as visitReason
            from Appointment a
            join a.doctor d
            join d.user doctorUser
            join a.patient p
            join p.user patientUser
            join a.slot s
            where a.doctor.id = :doctorId
              and a.appointmentDate = :date
              and a.status in (
                  com.smarthospital.appointment.AppointmentStatus.BOOKED,
                  com.smarthospital.appointment.AppointmentStatus.CHECKED_IN)
            order by s.startsAt asc, a.id asc
            """)
    List<DashboardAppointmentProjection> findScheduleForDoctor(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    long countByAppointmentDate(LocalDate appointmentDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :appointmentId")
    Optional<Appointment> findByIdForUpdate(@Param("appointmentId") Long appointmentId);
}
