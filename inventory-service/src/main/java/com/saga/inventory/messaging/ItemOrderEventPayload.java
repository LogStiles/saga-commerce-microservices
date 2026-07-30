package com.saga.inventory.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ItemOrderEventPayload(Integer itemId, Integer quantity, ItemOrderRequestType type) {

    public boolean isRequest() {
        return type.isRequest();
    }
}
