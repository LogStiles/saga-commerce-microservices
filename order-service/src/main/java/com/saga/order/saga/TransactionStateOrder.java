package com.saga.order.saga;

public enum TransactionStateOrder {
        PAYMENT("payment") {
            @Override
            public TransactionStateOrder next() {
                return INVENTORY;
            }

            @Override
            public TransactionStateOrder prev() {
                return null;
            }
        },
        INVENTORY("inventory") {
            @Override
            public TransactionStateOrder next() {
                return null;
            }

            @Override
            public TransactionStateOrder prev() {
                return PAYMENT;
            }
        };

        public final String topic;
        TransactionStateOrder(String topic) { this.topic = topic; }
        abstract TransactionStateOrder next();
        abstract TransactionStateOrder prev();

        static TransactionStateOrder startStep() { return PAYMENT; }
        
        static TransactionStateOrder next(String topic) {
            if (topic == null) {
                return startStep();
            }
            for (var t : values()) {
                if (t.topic.equals(topic)) {
                    return t.next();
                }
            }
            return null;
        }

        static TransactionStateOrder prev(String topic) {
            if (topic == null) {
                return null;
            }
            for (var t : values()) {
                if (t.topic.equals(topic)) {
                    return t.prev();
                }
            }
            return null;
        }

    }