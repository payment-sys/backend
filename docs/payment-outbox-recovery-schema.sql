ALTER TABLE payment_outbox DROP INDEX idx_payment_outbox_publish;

ALTER TABLE payment_outbox
    ADD COLUMN processing_started_at datetime(6) NULL AFTER created_at,
    ADD COLUMN published_at datetime(6) NULL AFTER processing_started_at,
    DROP COLUMN attempt_count,
    DROP COLUMN last_error_code,
    DROP COLUMN last_error_message,
    DROP COLUMN next_attempt_time;

CREATE INDEX idx_payment_outbox_publish
    ON payment_outbox (status, created_at, payment_outbox_id);
