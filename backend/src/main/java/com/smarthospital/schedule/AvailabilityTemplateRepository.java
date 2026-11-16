package com.smarthospital.schedule;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityTemplateRepository extends JpaRepository<AvailabilityTemplate, Long> {

    List<AvailabilityTemplate> findByDoctor_IdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

    List<AvailabilityTemplate> findByDoctor_IdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
            Long doctorId,
            int dayOfWeek);
}
