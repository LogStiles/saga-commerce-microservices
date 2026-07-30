CREATE TABLE IF NOT EXISTS outboxevent
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    timestamp     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aggregatetype VARCHAR(100) NOT NULL,
    aggregateid   VARCHAR(100) NOT NULL,
    type          VARCHAR(100) NOT NULL,
    payload       JSONB        NOT NULL
);

-- Debezium needs the full row image in the WAL so the EventRouter SMT can read
-- the payload/aggregate columns on every change.
ALTER TABLE outboxevent
    REPLICA IDENTITY FULL;
