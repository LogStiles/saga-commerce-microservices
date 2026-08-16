package com.saga.order.framework;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.Instant;

import static lombok.AccessLevel.PRIVATE;

/**
 * EventLog is an entity representing an incoming event from an inbox topic, and the eventlog table tracks these events.
 * Ensures idempotency by remembering what events have already been processed, turning Kafka messages received "at least once" into "effectively once"
 * Caveat: Our idempotency works by checking for the UUID on the Outbox row, so if the OutboxEventDispatcher accidentally created an outbox row for the same event twice, that event would have two different UUIDs and be processed twice
 */
@Entity
@Table(name = "eventlog")
@NoArgsConstructor(access = PRIVATE, force = true)
public class EventLog {
    @Id
    private final UUID eventId;

    private final Instant issuedOn;

    public EventLog(UUID eventId) {
        this.eventId = eventId;
        this.issuedOn = Instant.now();
    }
}