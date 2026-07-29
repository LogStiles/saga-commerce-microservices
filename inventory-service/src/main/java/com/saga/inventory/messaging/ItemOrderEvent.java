package com.saga.inventory.messaging;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

public final class ItemOrderEvent implements OutboxEvent<UUID, JsonNode> {
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
        return "item-order";
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
