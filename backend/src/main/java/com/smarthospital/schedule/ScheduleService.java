package com.smarthospital.schedule;

import com.smarthospital.common.ApiException;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final Clock clock;
    private final ZoneId hospitalZoneId;

    public ScheduleService(
            DoctorProfileRepository doctorProfileRepository,
            ScheduleSlotRepository scheduleSlotRepository,
            Clock clock,
            ZoneId hospitalZoneId) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
        this.clock = clock;
        this.hospitalZoneId = hospitalZoneId;
    }

    @Transactional
    public ScheduleSlotResponse createSlot(CreateScheduleSlotRequest request) {
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SLOT_TIME_INVALID",
                    "Slot start must be before slot end");
        }
        if (!request.startsAt().isAfter(Instant.now(clock))) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SLOT_START_NOT_FUTURE",
                    "Slot start must be in the future");
        }

        // The doctor row is the per-doctor mutex for schedule creation. Every
        // creator locks it before checking overlaps, so concurrent partial
        // overlaps cannot both pass the range query.
        DoctorProfile doctor = doctorProfileRepository.findByIdForUpdate(request.doctorId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_NOT_FOUND",
                        "Doctor was not found"));
        if (!doctor.isActive()
                || !doctor.getUser().isActive()
                || !doctor.getDepartment().isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DOCTOR_INACTIVE",
                    "Doctor is inactive");
        }

        if (!scheduleSlotRepository.findActiveOverlappingByDoctor(
                        doctor.getId(),
                        request.startsAt(),
                        request.endsAt())
                .isEmpty()) {
            throw slotOverlap();
        }

        try {
            ScheduleSlot slot = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                    doctor,
                    request.startsAt(),
                    request.endsAt(),
                    normalizeReasonLabel(request.reasonLabel())));
            return toResponse(slot);
        } catch (DataIntegrityViolationException exception) {
            throw slotOverlap();
        }
    }

    @Transactional
    public ScheduleSlotResponse cancelSlot(Long slotId) {
        ScheduleSlot slot = scheduleSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "SLOT_NOT_FOUND",
                        "Schedule slot was not found"));
        if (slot.getStatus() != ScheduleSlotStatus.OPEN || !slot.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SLOT_NOT_CANCELLABLE",
                    "Schedule slot cannot be cancelled");
        }
        slot.cancel();
        return toResponse(scheduleSlotRepository.saveAndFlush(slot));
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlotResponse> listPatientSlots(Long doctorId, LocalDate date) {
        DoctorProfile doctor = doctorProfileRepository.findActiveDirectoryDoctor(doctorId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "DOCTOR_NOT_FOUND",
                        "Doctor was not found"));
        Instant rangeStart = date.atStartOfDay(hospitalZoneId).toInstant();
        Instant rangeEnd = date.plusDays(1).atStartOfDay(hospitalZoneId).toInstant();
        Instant now = Instant.now(clock);

        return scheduleSlotRepository.findActiveByDoctorAndStartsAtRange(
                        doctor.getId(),
                        rangeStart,
                        rangeEnd,
                        ScheduleSlotStatus.OPEN)
                .stream()
                .filter(slot -> slot.getStartsAt().isAfter(now))
                .map(this::toResponse)
                .toList();
    }

    private ScheduleSlotResponse toResponse(ScheduleSlot slot) {
        return new ScheduleSlotResponse(
                slot.getId(),
                slot.getDoctor().getId(),
                slot.getStartsAt(),
                slot.getEndsAt(),
                slot.getReasonLabel(),
                slot.getStatus());
    }

    private static String normalizeReasonLabel(String reasonLabel) {
        if (reasonLabel == null || reasonLabel.isBlank()) {
            return null;
        }
        return reasonLabel.trim();
    }

    private static ApiException slotOverlap() {
        return new ApiException(
                HttpStatus.CONFLICT,
                "SLOT_OVERLAP",
                "Schedule slot overlaps an existing slot for this doctor");
    }
}
