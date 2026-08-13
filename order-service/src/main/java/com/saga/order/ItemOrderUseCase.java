package com.saga.order;

import org.springframework.stereotype.Service;

import com.saga.order.saga.TransactionManager;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ItemOrderUseCase {
    
    private final TransactionManager transactionManager;
    private final Orders orders;

    public Order makeOrder(ItemOrderRequest request) {
        var order = orders.save(request.toOrder());
        transactionManager.begin(order);
        return order; //@Transactional annotation so staged writes to the orders table, the transactionState table, and order-service's outboxevent table occur at end of method to ensure atomicity
    }
}
