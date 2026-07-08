package com.saga.order.messaging;

import com.saga.order.saga.TransactionManager;

import jakarta.transaction.Transactional;

import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class OrderPlacementEventHandler {
    private final TransactionManager transactionManager;

    @Transactional
    public void onInventoryEvent(UUID transactionId, UUID eventId, InventoryEvent payload) {
        var transaction = transactionManager.find(transactionId);
        if (transaction == null) {
            return;
        }
        transaction.onInventoryEvent(eventId, payload);
    }

    @Transactional
    public void onPaymentEvent(UUID transactionId, UUID eventId, PaymentEvent payload) {
        var transaction = transactionManager.find(transactionId);
        if (transaction == null) {
            return;
        }
        transaction.onPaymentEvent(eventId, payload);
    }
}
