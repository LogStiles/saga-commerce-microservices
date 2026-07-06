package com.saga.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import jakarta.persistence.EntityManager;

import org.hibernate.Session;

import org.springframework.transaction.annotation.Transactional;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;

public class OutboxEventDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(OutboxEventDispatcher.class);

    private final EntityManager entityManager;
    private final boolean removeAfterInsert;

    OutboxEventDispatcher(EntityManager entityManager) {
        this(entityManager, true);
    }

    OutboxEventDispatcher(EntityManager entityManager, boolean removeAfterInsert) {
        this.entityManager = entityManager;
        this.removeAfterInsert = removeAfterInsert;
    }

    @EventListener
    @Transactional(propagation = MANDATORY)
    public void on(OutboxEvent<?,?> event) {
        try (var session = entityManager.unwrap(Session.class)) {
            logger.info("Exported event found for type {}", event.getType());

            //Unwrap to Hibernate session and save
            var outbox = new Outbox(event);
            session.persist(outbox);

            // Useful for debugging
            if (removeAfterInsert) {
                session.remove(outbox);
            }
        }
    }
}
