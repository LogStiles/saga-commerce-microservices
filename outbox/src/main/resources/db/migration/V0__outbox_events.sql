-- Every microservice gets the outbox as a dependency in their pom.xml.
-- By defining the migration schema in outbox jar we can have changes in outbox automatically roll across all services that use it.
-- Flyway scans the whole classpath for migration files, and so it will find the V0__outbox_events.sql inside the outbox jar even if it's not in the service's resource folder.
CREATE TABLE IF NOT EXISTS outboxevent
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    timestamp     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aggregatetype VARCHAR(100) NOT NULL,
    aggregateid   VARCHAR(100) NOT NULL,
    type          VARCHAR(100) NOT NULL,
    payload       JSONB        NOT NULL
);

-- Column names match the defaults of Debezium's EventRouter SMT becuase we haven't overridden those defaults in *-outbox-connector.json, avoids infrastructure headaches
-- Debezium needs the full row image in the WAL so the EventRouter SMT can read
-- the payload/aggregate columns on every change.
ALTER TABLE outboxevent
    REPLICA IDENTITY FULL;
