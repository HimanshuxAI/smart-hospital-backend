package com.smarthospital.queue;

import com.smarthospital.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "queue_audits")
public class QueueAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "queue_entry_id", nullable = false)
    private QueueEntry queueEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private User actor;

    @Column(name = "operation", nullable = false, length = 50)
    private String operation;

    @Column(name = "previous_value", length = 255)
    private String previousValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected QueueAudit() {
    }

    public QueueAudit(
            QueueEntry queueEntry,
            User actor,
            String operation,
            String previousValue,
            String newValue,
            String reason) {
        this.queueEntry = Objects.requireNonNull(queueEntry, "queueEntry must not be null");
        this.actor = Objects.requireNonNull(actor, "actor must not be null");
        this.operation = Objects.requireNonNull(operation, "operation must not be null");
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        if (this.reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public QueueEntry getQueueEntry() {
        return queueEntry;
    }

    public User getActor() {
        return actor;
    }

    public String getOperation() {
        return operation;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
