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

/**
 * ItemOrderEventHandler receives messages from ItemOrderInboxEventConsumer at least once and ensures they are processed with idempotency
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ItemOrderEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(ItemOrderEventHandler.class);

    private final ApplicationEventPublisher eventPublisher;
    private final EventLogs eventLogs;
    private final Inventory inventory;

    public void onItemOrderEvent(UUID sagaId, UUID eventId, ItemOrderEventPayload payload) {
        if (eventLogs.isAlreadyProcessed(eventId)) { //Idempotency check. If eventId is already found in eventLogs, do nothing
            logger.info("Event with UUID {} was already retrieved.", eventId);
            return;
        }

        var orderedItem = inventory.findById(new Item.ItemId(payload.itemId()));

        final ItemOrderStatus status; //use business logic to determine ItemOrderStatus
        if (payload.isRequest() || orderedItem.isEmpty()) {
            if (orderedItem.isEmpty() || !orderedItem.get().isAvailableInQuantity(payload.quantity())) { //does item exist and does item have enough in stock for the ItemOrderEventPayload
                status = ItemOrderStatus.REJECTED;
            } else {
                orderedItem.get().reserveItem(payload.quantity()); //happy path, remove item from stock
                status = ItemOrderStatus.PURCHASED;
            }
        } else { //ItemOrderRequestType was not REQUEST, and so it must be CANCEL
            orderedItem.get().releaseItem(payload.quantity()); //compensating transaction, put item back in stock
            status = ItemOrderStatus.CANCELLED;
        }
        /** 
         *  @Transactional enforces that all 3 possible writes have atomicity
         *  We don't do any explicit writes to Inventory.java, but Hibernate will see that the orderedItem.stockAmount needs to be updated at commit if we called releaseItem() or reserveItem()
        */
        eventPublisher.publishEvent(ItemOrderEvent.of(sagaId, status));
        eventLogs.processed(eventId);
    }
}
