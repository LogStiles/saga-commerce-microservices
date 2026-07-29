package com.saga.inventory.messaging;

public enum ItemOrderRequestType {
    REQUEST, CANCEL;

    public boolean isRequest() {
        return this == REQUEST;
    }
}
