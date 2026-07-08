package com.saga.order.messaging;

import com.saga.order.framework.TransactionStepStatus;

public enum InventoryStatus {
    PURCHASED, REJECTED, CANCELLED;

    public TransactionStepStatus toTransactionStepStatus() {
        if (this == PURCHASED) {
            return TransactionStepStatus.SUCCEEDED;
        }
        else if (this == REJECTED) {
            return TransactionStepStatus.FAILED;
        }
        else { //CANCELLED
            return TransactionStepStatus.COMPENSATED;
        }
    }
}
