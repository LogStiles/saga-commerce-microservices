package com.saga.inventory.messaging;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.saga.inventory.Item;
import com.saga.inventory.ItemOrderEventHandler;

import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;

/**
 * ItemOrderInboxEventConsumer listens to inventory-service's inbox for messages, logs them, and calls the ItemOrderEventHandler to handle them
 */
@KafkaListener(
    topics = "${kafka.topic.inbox.events.name}",
    groupId = "${spring.kafka.consumer.group-id}",
    containerFactory = "kafkaListenerContainerFactory" //same name as ConcurrentKafkaListenerContainerFactory returning method in KafkaConfig.java
)
@Component
@RequiredArgsConstructor
public class ItemOrderInboxEventConsumer {
    private final Logger logger = LoggerFactory.getLogger(ItemOrderInboxEventConsumer.class);

    private final ItemOrderEventHandler itemOrderEventHandler;

    @KafkaHandler
    void handle(@Header(RECEIVED_KEY) UUID transactionId,
                @Header("id") String eventId,
                @Header("eventType") String eventType,
                @Payload ItemOrderEventPayload payload) {

        logger.debug("Kafka message with key = {}, eventType = {}, payload = {}", transactionId, eventType, payload);
        itemOrderEventHandler.onItemOrderEvent(transactionId, UUID.fromString(eventId), payload);
    }
}
