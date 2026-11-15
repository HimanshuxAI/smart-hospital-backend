package com.smarthospital.queue;

import com.smarthospital.ClinicalTestSupport;
import com.smarthospital.appointment.Appointment;
import com.smarthospital.appointment.AppointmentRepository;
import com.smarthospital.doctor.DoctorProfile;
import com.smarthospital.schedule.ScheduleSlot;
import com.smarthospital.schedule.ScheduleSlotRepository;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public abstract class QueueTestSupport extends ClinicalTestSupport {

    private static final AtomicInteger SLOT_SEQUENCE = new AtomicInteger();

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    protected QueueEntryRepository queueEntryRepository;

    protected QueueEntry createQueueEntry(
            TestPatient patient,
            TestDoctor doctor,
            EmergencyTier tier,
            Instant arrivedAt) {
        DoctorProfile doctorProfile = doctorProfileRepository.findById(doctor.doctorId()).orElseThrow();
        int sequence = SLOT_SEQUENCE.incrementAndGet();
        Instant startsAt = NOW.plusSeconds(sequence * 1800L);
        ScheduleSlot slot = scheduleSlotRepository.saveAndFlush(new ScheduleSlot(
                doctorProfile,
                startsAt,
                startsAt.plusSeconds(1800)));
        slot.markBooked();
        scheduleSlotRepository.saveAndFlush(slot);

        Appointment appointment = appointmentRepository.saveAndFlush(new Appointment(
                patientProfileRepository.findById(patient.patientId()).orElseThrow(),
                doctorProfile,
                slot,
                TODAY,
                "Queue workflow test visit"));
        appointment.checkIn(NOW);
        appointmentRepository.saveAndFlush(appointment);

        int tokenNumber = queueEntryRepository.findMaximumTokenNumber(
                doctor.doctorId(),
                TODAY) + 1;
        return queueEntryRepository.saveAndFlush(new QueueEntry(
                appointment,
                doctorProfile,
                TODAY,
                tokenNumber,
                tier,
                arrivedAt));
    }
}
