package com.saga.payment.messaging;

import com.saga.outbox.OutboxEvent;

import com.saga.payment.PaymentStatus;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.UUID;
import java.time.Instant;

public final class PaymentEvent implements OutboxEvent<String, JsonNode> {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final UUID transactionId;
    private final JsonNode payload;
    private final Instant timestamp;

    private PaymentEvent(UUID transactionId, JsonNode payload) {
        this.transactionId = transactionId;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    public static PaymentEvent of(UUID transactionId, PaymentStatus status) {
        ObjectNode asJson = mapper.createObjectNode().put("status", status.name());
        return new PaymentEvent(transactionId, asJson);
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(transactionId);
    }

    @Override
    public String getAggregateType() {
        return "payment";
    }

    @Override
    public String getType() {
        return "PaymentUpdated";
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
