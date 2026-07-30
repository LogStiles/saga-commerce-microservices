package com.saga.order.saga;

import java.time.Instant;
import java.util.UUID;

import com.saga.order.messaging.PaymentEvent;
import com.saga.order.messaging.PaymentStatus;
import com.saga.outbox.OutboxEvent;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class TransactionEvent implements OutboxEvent<UUID, JsonNode> {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final UUID transactionId;
    private final JsonNode payload;
    private final Instant timestamp;
    private final String aggregateType;
    private final String eventType;

    TransactionEvent(UUID transactionId, String aggregateType, String eventType, JsonNode payload) {
        this.transactionId = transactionId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    @Override
    public UUID getAggregateId() {
        return this.transactionId;
    }

    @Override
    public String getAggregateType() {
        return this.aggregateType;
    }

    @Override
    public String getType() {
        return this.eventType;
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public JsonNode getPayload() {
        return payload;
    }
}
