CREATE TABLE carts (
    id                     UUID          PRIMARY KEY,
    status                 VARCHAR(20)   NOT NULL,
    currency               VARCHAR(3)    NOT NULL,
    total_amount           NUMERIC(12, 2) NOT NULL,
    created_at             TIMESTAMPTZ   NOT NULL,
    expires_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL,
    payment_method_name    VARCHAR(60),
    payment_method_type    VARCHAR(20),
    payment_transaction_id VARCHAR(64),
    payment_last_digits    VARCHAR(4),
    payment_pix_code       VARCHAR(512),
    payment_amount         NUMERIC(12, 2),
    payment_currency       VARCHAR(3),
    paid_at                TIMESTAMPTZ,
    CONSTRAINT ck_carts_status CHECK (status IN ('CREATED', 'PROCESSING', 'APPROVED', 'DECLINED'))
);

CREATE INDEX idx_carts_status_expires_at ON carts (status, expires_at);

CREATE TABLE cart_items (
    cart_id     UUID           NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    item_order  INTEGER        NOT NULL,
    slug        VARCHAR(60)    NOT NULL,
    name        VARCHAR(120)   NOT NULL,
    description VARCHAR(255),
    price       NUMERIC(12, 2) NOT NULL,
    currency    VARCHAR(3)     NOT NULL,
    PRIMARY KEY (cart_id, item_order)
);

CREATE TABLE cart_payment_methods (
    cart_id      UUID        NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    method_order INTEGER     NOT NULL,
    name         VARCHAR(60) NOT NULL,
    type         VARCHAR(20) NOT NULL,
    PRIMARY KEY (cart_id, method_order)
);
