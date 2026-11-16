package com.smarthospital.queue;

import com.smarthospital.appointment.Appointment;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.doctor.DoctorProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final QueueEntryRepository queueEntryRepository;

    public TokenService(
            DoctorProfileRepository doctorProfileRepository,
            QueueEntryRepository queueEntryRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.queueEntryRepository = queueEntryRepository;
    }

    /**
     * Reserves the next token while holding the per-doctor database mutex. The
     * caller must use the returned locked doctor for the rest of the
     * doctor -> appointment -> queue-entry lock sequence.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public TokenAllocation reserve(Long doctorId, LocalDate queueDate) {
        DoctorProfile doctor = doctorProfileRepository.findByIdForUpdate(doctorId)
                .orElseThrow();
        int nextToken = queueEntryRepository.findMaximumTokenNumber(doctorId, queueDate) + 1;
        return new TokenAllocation(doctor, nextToken);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public QueueEntry createEntry(
            Appointment appointment,
            DoctorProfile doctor,
            LocalDate queueDate,
            int tokenNumber,
            Instant arrivedAt) {
        return queueEntryRepository.saveAndFlush(new QueueEntry(
                appointment,
                doctor,
                queueDate,
                tokenNumber,
                EmergencyTier.NORMAL,
                arrivedAt));
    }

    public record TokenAllocation(DoctorProfile doctor, int tokenNumber) {
    }
}
