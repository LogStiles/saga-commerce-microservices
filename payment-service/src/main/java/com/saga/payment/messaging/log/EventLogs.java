package com.saga.payment.messaging.log;

import org.springframework.stereotype.Repository;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

/**
 * EventLogs is the table that tracks incoming events from the inbox topic.
 * Ensures idempotency by remembering what events have already been processed
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
