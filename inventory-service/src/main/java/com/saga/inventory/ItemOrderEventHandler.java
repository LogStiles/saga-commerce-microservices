package com.saga.inventory;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.saga.inventory.messaging.ItemOrderEvent;
import com.saga.inventory.messaging.ItemOrderEventPayload;
import com.saga.inventory.messaging.ItemOrderStatus;
import com.saga.inventory.messaging.log.EventLogs;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ItemOrderEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ItemOrderEventHandler.class);

    private final ApplicationEventPublisher eventPublisher;
    private final EventLogs eventLogs;
    private final Inventory inventory;

    public void onItemOrderEvent(UUID sagaId, UUID eventId, ItemOrderEventPayload payload) {
        if (eventLogs.isAlreadyProcessed(eventId)) {
            logger.info("Event with UUID {} was already retrieved.", eventId);
            return;
        }

        var orderedItem = inventory.findById(new Item.ItemId(payload.itemId()));

        final ItemOrderStatus status;
        if (payload.isRequest() || orderedItem.isEmpty()) {
            if (orderedItem.isEmpty() || !orderedItem.get().isAvailableInQuantity(payload.quantity())) {
                status = ItemOrderStatus.REJECTED;
            } else {
                orderedItem.get().reserveItem(payload.quantity());
                status = ItemOrderStatus.ORDERED;
            }
        } else {
            orderedItem.get().releaseItem(payload.quantity());
            status = ItemOrderStatus.CANCELLED;
        }

        eventPublisher.publishEvent(ItemOrderEvent.of(sagaId, status));
        eventLogs.proccessed(eventId);
    }
}
