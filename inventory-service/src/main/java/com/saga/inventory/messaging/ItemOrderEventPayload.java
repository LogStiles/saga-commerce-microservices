package com.saga.inventory.messaging;

public record ItemOrderEventPayload(Integer itemId, Integer quantity, ItemOrderRequestType type) {

    public boolean isRequest() {
        return type.isRequest();
    }
}
