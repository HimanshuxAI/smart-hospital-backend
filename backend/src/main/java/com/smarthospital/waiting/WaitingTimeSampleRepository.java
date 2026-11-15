package com.smarthospital.waiting;

import com.smarthospital.queue.EmergencyTier;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitingTimeSampleRepository extends JpaRepository<WaitingTimeSample, Long> {

    List<WaitingTimeSample> findByDoctor_IdAndQueueDateOrderByRecordedAtAsc(Long doctorId, LocalDate queueDate);

    List<WaitingTimeSample> findByDoctor_IdAndQueueDateAndTriageTierOrderByRecordedAtAsc(
            Long doctorId,
            LocalDate queueDate,
            EmergencyTier triageTier);
}
