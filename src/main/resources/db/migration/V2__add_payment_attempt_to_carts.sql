ALTER TABLE carts
    ADD COLUMN payment_attempt_id    UUID,
    ADD COLUMN processing_started_at TIMESTAMPTZ;

ALTER TABLE carts
    ADD CONSTRAINT ck_carts_processing_attempt
        CHECK (status <> 'PROCESSING' OR (payment_attempt_id IS NOT NULL AND processing_started_at IS NOT NULL));
