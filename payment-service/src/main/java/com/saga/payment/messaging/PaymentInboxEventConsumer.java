package com.saga.payment.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import com.saga.payment.Payment;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;

@KafkaListener(
    topics = "${kafka.topic.inbox.events.name}",
    groupId = "${spring.kafka.consumer.group-id}",
    containerFactory = "kafkaListenerContainerFactory"
)
@Component
@RequiredArgsConstructor
public class PaymentInboxEventConsumer {
    private final Logger logger = LoggerFactory.getLogger(PaymentInboxEventConsumer.class);

    private final PaymentEventHandler paymentEventHandler;

    @KafkaHandler
    void handle(@Header(RECEIVED_KEY) UUID transactionId,
                @Header("id") String eventId,
                @Header("eventType") String eventType,
                @Payload Payment payload) {

        logger.debug("Kafka message with key = {}, eventType = {}, payload = {}", transactionId, eventType, payload);
        paymentEventHandler.onPaymentEvent(UUID.fromString(eventId), transactionId, payload);
    }
}
