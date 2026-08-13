package com.saga.order.framework;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Transaction represents a multi-event processing of an order
 * Meant to be extended, this class provides idempotency infrastructure for processing events that arrive at least once from kafka topics
 * PayloadType represents whether or not the transaction is on the happy path (REQUEST) or not (CANCEL)
 */
@RequiredArgsConstructor
public abstract class Transaction {
    private final EntityManager entityManager;

    public enum PayloadType {
        REQUEST, CANCEL;
    }

    /**
     * Idempotency check, does nothing if eventId is in EventLog table
     * @param eventId primary key for EventLog table row
     * @param callback Runnable that processes the transaction step associated with the eventId
     */
    protected void ensureProcessed(UUID eventId, Runnable callback) {
        if (entityManager.find(EventLog.class, eventId) != null) {
            return;
        }

        callback.run();
        entityManager.persist(new EventLog(eventId)); //persist AFTER callback is run so that only processed events go in EventLog
    }
}
