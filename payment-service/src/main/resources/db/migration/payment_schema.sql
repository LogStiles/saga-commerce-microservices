CREATE TABLE IF NOT EXISTS payment {
    purchase_id UUID PRIMARY KEY,
    timestamp       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    shopper_id      INT         NOT NULL,
    payment_amount  BIGINT      NOT NULL,
    credit_card_num VARCHAR(16) NOT NULL,
    type            VARCHAR(20) NOT NULL
};