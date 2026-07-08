package com.saga.order.framework;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class Transaction {
    private final EntityManager entityManager;

    public enum PayloadType {
        REQUEST, CANCEL;
    }

    protected void ensureProcessed(UUID eventId, Runnable callback) {
        if (entityManager.find(EventLog.class, eventId) != null) {
            return;
        }

        callback.run();
        entityManager.persist(new EventLog(eventId));
    }
}
