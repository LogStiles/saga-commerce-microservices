package com.saga.payment;

public enum PaymentRequestType {
    REQUEST, CANCEL;

    public boolean isRequest() {
        return this == REQUEST;
    }
}
