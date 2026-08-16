package com.saga.order.messaging;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;

/**
 * KafkaOrderConsumer takes messages from the kafka topics associated with each transaction step and delegates them to their equivalent method in OrderPlacementEventHandler
 */
@Component
@RequiredArgsConstructor
public class KafkaOrderConsumer {
    private final String EVENT_ID = "id";
    private final String EVENT_TYPE = "eventType";

    private final Logger logger = LoggerFactory.getLogger(KafkaOrderConsumer.class);
    private final OrderPlacementEventHandler orderPlacementEventHandler;

    @KafkaListener(topics = "${kafka.topic.saga.payment.inbox.events}", containerFactory = "paymentKLCFactory") //same name as ConcurrentKafkaListenerContainerFactory returning method in KafkaConfig.java
    void listen(@Header(RECEIVED_KEY) UUID transactionId,
                @Header(EVENT_ID) String eventId,
                @Header(EVENT_TYPE) String eventType,
                @Payload PaymentEvent payload) {
        logger.info("Kafka message with key = {}, eventType {} arrived, {}", transactionId, eventType, payload);
        orderPlacementEventHandler.onPaymentEvent(transactionId, UUID.fromString(eventId), payload);
    }

    @KafkaListener(topics = "${kafka.topic.saga.inventory.inbox.events}", containerFactory = "inventoryKLCFactory") //same name as ConcurrentKafkaListenerContainerFactory returning method in KafkaConfig.java
    void listen(@Header(RECEIVED_KEY) UUID transactionId,
                @Header(EVENT_ID) String eventId,
                @Header(EVENT_TYPE) String eventType,
                @Payload InventoryEvent payload) {
        logger.info("Kafka message with key = {}, eventType {} arrived, {}", transactionId, eventType, payload);
        orderPlacementEventHandler.onInventoryEvent(transactionId, UUID.fromString(eventId), payload);
    }

}
