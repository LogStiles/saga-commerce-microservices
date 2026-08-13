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

/**
 * TransactionSaga orchestrates the steps in a transaction with a saga design pattern.
 * Determines the health of the overall saga
 * Publishes events asking for transaction steps to be started or compensated for to Spring's ApplicationEventPublisher
 */
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

    // Invoked when KafkaOrderConsumer receives a message from the property referenced in @KafkaListener kafka.topic.saga.payment.inbox.events, handled by OrderPlacementEventHandler
    public void onPaymentEvent(UUID eventId, PaymentEvent payload) {
        ensureProcessed(eventId, () -> {
            onStepEvent(TransactionStateOrder.PAYMENT.topic, payload.status().toTransactionStepStatus());
            updateOrderStatus();
        });
    }

    // Invoked when KafkaOrderConsumer receives a message from the property referenced in @KafkaListener kafka.topic.saga.inventory.inbox.events, handled by OrderPlacementEventHandler
    public void onInventoryEvent(UUID eventId, InventoryEvent payload) { 
        ensureProcessed(eventId, () -> {
            onStepEvent(TransactionStateOrder.INVENTORY.topic, payload.status().toTransactionStepStatus());
            updateOrderStatus();
        });
    }

    /**
     * Checks if the transaction's associated order has either SUCCEEDED or FAILED based on the TransactionState
     * No else statement means that all orders remain PENDING until the order is completed
     */
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

    /**
     * The saga orchestration works by moving along a 1-D axis of steps each transaction must take
     * It moves forward on the axis on the happy path and backwards when something fails to request compensating transactions
     * @param step Represents what step the saga is on
     * @param status Represents the status of the step (STARTED, FAILED, SUCCEEDED, COMPENSATING, COMPENSATED)
     */
    private void onStepEvent(String step, TransactionStepStatus status) {
        state.updateStepStatus(step, status);

        if (status == SUCCEEDED) {
            advance();
        } else if (status == FAILED || status == COMPENSATED) {
            goBack();
        }

        state.advanceTransactionStatus();
    }

    /*
        Moves the saga state system along the happy path
    */
    private void advance() {
        var next = TransactionStateOrder.next(state.getCurrentStep());
        if (next == null) { //if next is null we've reached the end of the happy path
            state.setCurrentStep(null);
            return;
        }

        eventPublisher.publishEvent(new TransactionEvent(state.getId(), next.topic, REQUEST.name(), state.getPayload()));

        state.updateStepStatus(next.topic, STARTED);
        state.setCurrentStep(next.topic);
    }

    /*
        Something went wrong and we have to perform compensating transactions
    */
    private void goBack() {
        var prev = TransactionStateOrder.prev(state.getCurrentStep());
        if (prev == null) { // if prev is null we are at the beginning and there are no more compensating transactions to perform
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
