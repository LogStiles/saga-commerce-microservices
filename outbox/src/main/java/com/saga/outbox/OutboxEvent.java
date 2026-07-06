package com.saga.outbox;

import java.time.Instant;

public interface OutboxEvent<ID, P> {
    ID getAggregateId();

    String getAggregateType();

    String getType();

    Instant getTimestamp();

    P getPayload();
}
