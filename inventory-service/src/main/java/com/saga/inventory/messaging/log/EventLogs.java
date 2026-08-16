package com.saga.inventory.messaging.log;

import org.springframework.stereotype.Repository;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;
/**
 * EventLogs is the table that tracks incoming events from the inbox topic.
 * Ensures idempotency by remembering what events have already been processed, turning Kafka messages received "at least once" into "effectively once"
 * Caveat: Our idempotency works by checking for the UUID on the Outbox row, so if the OutboxEventDispatcher accidentally created an outbox row for the same event twice, that event would have two different UUIDs and be processed twice
 */
@Repository
public interface EventLogs extends CrudRepository<EventLog, UUID> {
    default void processed(UUID eventId) {
        save(new EventLog(eventId));
    }

    default boolean isAlreadyProcessed(UUID eventId) {
        return this.existsById(eventId);
    }
}
