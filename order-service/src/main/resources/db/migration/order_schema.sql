CREATE TABLE IF NOT EXISTS reservation
(
    id              UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    timestamp       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    item_id         BIGINT         NOT NULL,
    quantity        BIGINT         NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    shopper_id      BIGINT         NOT NULL,
    payment_due     BIGINT         NOT NULL,
    credit_card_num VARCHAR(16)    NOT NULL
);