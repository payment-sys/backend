ALTER TABLE orders
    ADD COLUMN quantity_change_success_product_ids JSON NULL,
    ADD COLUMN quantity_change_failed_product_ids JSON NULL;

ALTER TABLE quantity_change_outbox
    ADD COLUMN outbox_type VARCHAR(32) NOT NULL DEFAULT 'DECREASE';

ALTER TABLE quantity_change_outbox
    ADD CONSTRAINT uk_quantity_change_outbox_order_type UNIQUE (order_code, outbox_type);
