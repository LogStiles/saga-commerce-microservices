package com.saga.payment.messaging;

import com.saga.payment.Payment;
import com.saga.payment.Payments;
import com.saga.payment.messaging.log.EventLogs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * PaymentEventHandler receives messages from PaymentInboxEventConsumer at least once and ensures they are processed with idempotency
 */
@Component
@Transactional
@RequiredArgsConstructor
public class PaymentEventHandler {
    private final Logger logger = LoggerFactory.getLogger(PaymentEventHandler.class);

    private final ApplicationEventPublisher eventPublisher;
    private final EventLogs eventLogs;
    private final Payments payments;

    public void onPaymentEvent(UUID eventId, UUID transactionId, Payment event) {
        if (eventLogs.isAlreadyProcessed(eventId)) { //Idempotency check. If eventId is already found in eventLogs, do nothing
            logger.info("Event with UUID {} was already retrieved.", eventId);
            return;
        } 
        // @Transactional enforces that all 3 of these writes have atomicity
        payments.save(event);
        eventPublisher.publishEvent(PaymentEvent.of(transactionId, event.paymentStatus()));
        eventLogs.processed(eventId);
    }
}
