package com.saga.inventory.messaging;

import java.time.Instant;
import java.util.UUID;

import com.saga.outbox.OutboxEvent;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class ItemOrderEvent implements OutboxEvent<String, JsonNode> {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final UUID transactionId;
    private final JsonNode payload;
    private final Instant timestamp;

    private ItemOrderEvent(UUID transactionId, JsonNode payload) {
        this.transactionId = transactionId;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    public static ItemOrderEvent of(UUID transactionId, ItemOrderStatus status) {
        ObjectNode asJson = mapper.createObjectNode().put("status", status.name());
        return new ItemOrderEvent(transactionId, asJson);
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(transactionId);
    }

    @Override
    public String getAggregateType() {
        // Routed by the inventory Debezium connector to "inventory.outbox.events",
        // which the order-service orchestrator listens on for the inventory step.
        return "inventory";
    }

    @Override
    public String getType() {
        return "InventoryUpdated";
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
