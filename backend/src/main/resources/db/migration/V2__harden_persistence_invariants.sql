ALTER TABLE appointments
    ADD COLUMN checked_in_at DATETIME(6) NULL,
    ADD COLUMN completed_at DATETIME(6) NULL,
    ADD COLUMN no_show_at DATETIME(6) NULL;

ALTER TABLE schedule_slots
    ADD COLUMN reason_label VARCHAR(255) NULL;

ALTER TABLE queue_audits
    DROP FOREIGN KEY fk_queue_audits_entry;

ALTER TABLE queue_audits
    ADD CONSTRAINT fk_queue_audits_entry
        FOREIGN KEY (queue_entry_id) REFERENCES queue_entries (id) ON DELETE RESTRICT;

CREATE TABLE seed_locks (
    id TINYINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO seed_locks (id) VALUES (1);
