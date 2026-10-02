UPDATE product_quantity_event
SET status = 'READY'
WHERE status = 'RETRY';

ALTER TABLE product_quantity_event DROP INDEX idx_product_quantity_event_status_next_attempt_id;

ALTER TABLE product_quantity_event
    DROP COLUMN retry_count,
    DROP COLUMN next_attempt_time;
