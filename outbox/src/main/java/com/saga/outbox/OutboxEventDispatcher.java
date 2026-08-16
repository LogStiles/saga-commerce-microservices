package com.saga.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import jakarta.persistence.EntityManager;

import org.hibernate.Session;

import org.springframework.transaction.annotation.Transactional;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;

/**
 * OutboxEventDispatcher listens for when events are written to Spring's ApplicationEventPublisher and writes them to an Outbox table
 * We need to both record an event within a service's database and send it via Kafka to other services with guaranteed atomicity
 * OutboxEventDispatcher solves that dual write problem by providing an outbox event table, which we can perform CDC on with Debezium
 */
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

    //Propagation is set to MANDATORY to enforce that on() cannot begin a Transactional context
    //on() only runs in an already existing Transactional context, to help with a dual-write problem. Throws an exception otherwise
    @EventListener
    @Transactional(propagation = MANDATORY)
    public void on(OutboxEvent<?,?> event) {
        try (var session = entityManager.unwrap(Session.class)) {
            logger.info("Exported event found for type {}", event.getType());

            //Unwrap to Hibernate session and save
            var outbox = new Outbox(event);
            session.persist(outbox);

            // Useful for debugging
            // Debezium uses Postgres's WAL to perform CDC rather than the table so we can remove the write right after we persist
            // Improves performance if removeAfterInsert is set to true
            if (removeAfterInsert) {
                session.remove(outbox);
            }
        }
    }
}
