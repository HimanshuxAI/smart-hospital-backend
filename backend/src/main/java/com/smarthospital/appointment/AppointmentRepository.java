package com.smarthospital.appointment;

import jakarta.persistence.LockModeType;
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

    List<Appointment> findByDoctor_IdAndAppointmentDateBetweenOrderByAppointmentDateAscIdAsc(
            Long doctorId,
            LocalDate startsAt,
            LocalDate endsAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a "
            + "join fetch a.patient "
            + "join fetch a.doctor "
            + "join fetch a.slot "
            + "where a.id = :appointmentId")
    Optional<Appointment> findByIdForUpdate(@Param("appointmentId") Long appointmentId);
}
