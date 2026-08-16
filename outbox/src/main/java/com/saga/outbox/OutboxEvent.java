package com.saga.outbox;

import java.time.Instant;

/**
 * OutboxEvent is a generic event we write to the Outbox table
 * @param <ID> the aggregate Id of the event, identifies what owns the event
 * @param <P> carries additional metadata and business logic (to be determined on implementation)
 */
public interface OutboxEvent<ID, P> {
    ID getAggregateId();

    String getAggregateType();

    String getType();

    Instant getTimestamp();

    P getPayload();
}
