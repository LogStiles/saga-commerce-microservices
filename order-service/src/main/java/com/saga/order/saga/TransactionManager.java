package com.saga.order.saga;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.saga.order.Order;
import com.saga.order.Orders;
import com.saga.order.framework.Transaction;
import com.saga.order.framework.TransactionState;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * TransactionManager initializes transactions with begin()
 * Owns both the table of user orders and the table that has the transactions associated with those orders 
 */
// Explicit bean name: the default ("transactionManager") collides with the
// PlatformTransactionManager that HibernateJpaConfiguration auto-configures.
@Component("sagaTransactionManager")
@RequiredArgsConstructor
public class TransactionManager {
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;
    private final Orders orders;

    public void begin(Order order) {
        var payload = order.toTransactionPayload().put("type", Transaction.PayloadType.REQUEST.name());
        var transactionState = new TransactionState("order", payload);
        entityManager.persist(transactionState); // persist the transactionState because we discard it at the end of begin(Order order)
        // the transaction starts up again once KafkaOrderConsumer receives messages and transactionState will pulled from the db by OrderPlacementEventHandler

        var transaction = new TransactionSaga(eventPublisher, entityManager, orders, transactionState);
        transaction.init();
    }

    public TransactionSaga find(UUID transactionId) {
        var state = entityManager.find(TransactionState.class, transactionId);
        if (state == null) { // received an unknown transactionId, not found in table
            return null;
        }

        return new TransactionSaga(eventPublisher, entityManager, orders, state);
    }
}
