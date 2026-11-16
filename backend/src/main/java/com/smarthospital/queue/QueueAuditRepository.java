package com.smarthospital.queue;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueAuditRepository extends JpaRepository<QueueAudit, Long> {

    List<QueueAudit> findByQueueEntry_IdOrderByCreatedAtAsc(Long queueEntryId);
}
