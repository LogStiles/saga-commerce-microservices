package com.saga.order.messaging;

import com.saga.order.framework.TransactionStepStatus;

public enum PaymentStatus {
    REQUESTED, CANCELLED, FAILED, COMPLETED;

    public TransactionStepStatus toTransactionStepStatus() {
        if (this == REQUESTED || this == COMPLETED) {
            return TransactionStepStatus.SUCCEEDED;
        }
        else if (this == CANCELLED) {
            return TransactionStepStatus.COMPENSATED;
        }
        else { //FAILED
            return TransactionStepStatus.FAILED;
        }
    }
}
