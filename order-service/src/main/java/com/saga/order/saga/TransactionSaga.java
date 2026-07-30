package com.saga.order.saga;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;

import com.saga.order.Order;
import com.saga.order.Orders;
import com.saga.order.saga.TransactionEvent;
import com.saga.order.framework.Transaction.PayloadType;
import com.saga.order.framework.Transaction;
import com.saga.order.framework.TransactionState;
import com.saga.order.framework.TransactionStepStatus;
import com.saga.order.messaging.InventoryEvent;
import com.saga.order.messaging.PaymentEvent;

import jakarta.persistence.EntityManager;
import tools.jackson.databind.node.ObjectNode;

import static com.saga.order.framework.Transaction.PayloadType.REQUEST;
import static com.saga.order.framework.Transaction.PayloadType.CANCEL;
import static com.saga.order.framework.TransactionStatus.ABORTED;
import static com.saga.order.framework.TransactionStatus.FINISHED;

import static com.saga.order.framework.TransactionStepStatus.STARTED;
import static com.saga.order.framework.TransactionStepStatus.SUCCEEDED;
import static com.saga.order.framework.TransactionStepStatus.COMPENSATED;
import static com.saga.order.framework.TransactionStepStatus.COMPENSATING;
import static com.saga.order.framework.TransactionStepStatus.FAILED;

public final class TransactionSaga extends Transaction {
    private final ApplicationEventPublisher eventPublisher;
    private final Orders orders;
    private final TransactionState state;

    TransactionSaga(ApplicationEventPublisher eventPublisher, EntityManager entityManager, Orders orders, TransactionState transactionState) {
        super(entityManager);
        this.eventPublisher = eventPublisher;
        this.orders = orders;
        this.state = transactionState;
    }

    public void init() {
        advance();
    }

    public void onPaymentEvent(UUID eventId, PaymentEvent payload) {
        ensureProcessed(eventId, () -> {
            onStepEvent(TransactionStateOrder.PAYMENT.topic, payload.status().toTransactionStepStatus());
            updateOrderStatus();
        });
    }

    public void onInventoryEvent(UUID eventId, InventoryEvent payload) { 
        ensureProcessed(eventId, () -> {
            onStepEvent(TransactionStateOrder.INVENTORY.topic, payload.status().toTransactionStepStatus());
            updateOrderStatus();
        });
    }

    private void updateOrderStatus() {
        var order = orders.findById(new Order.OrderId(getOrderId())).orElseThrow(RuntimeException::new);

        if (state.getTransactionStatus() == FINISHED) {
            order.markAsSucceeded();
        } else if (state.getTransactionStatus() == ABORTED) {
            order.markAsFailed();
        }
    }

    private UUID getOrderId() {
        return UUID.fromString(state.getPayload().get("orderId").asText());
    }

    private void onStepEvent(String step, TransactionStepStatus status) {
        state.updateStepStatus(step, status);

        if (status == SUCCEEDED) {
            advance();
        } else if (status == FAILED || status == COMPENSATED) {
            goBack();
        }

        state.advanceTransactionStatus();
    }

    private void advance() {
        var next = TransactionStateOrder.next(state.getCurrentStep());
        if (next == null) {
            state.setCurrentStep(null);
            return;
        }

        eventPublisher.publishEvent(new TransactionEvent(state.getId(), next.topic, REQUEST.name(), state.getPayload()));

        state.updateStepStatus(next.topic, STARTED);
        state.setCurrentStep(next.topic);
    }

    private void goBack() {
        var prev = TransactionStateOrder.prev(state.getCurrentStep());
        if (prev == null) {
            state.setCurrentStep(null);
            return;
        }

        var payload = ((ObjectNode) state.getPayload().deepCopy());
        payload.put("type", CANCEL.name());

        eventPublisher.publishEvent(new TransactionEvent(state.getId(), prev.topic, CANCEL.name(), payload));

        state.updateStepStatus(prev.topic, COMPENSATING);
        state.setCurrentStep(prev.topic);
    }
}
