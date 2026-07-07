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
        return order;
    }
}
