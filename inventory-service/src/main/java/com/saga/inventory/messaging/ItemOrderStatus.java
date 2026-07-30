package com.saga.inventory.messaging;


public enum ItemOrderStatus {
    // Names must match the order-service InventoryStatus enum so the orchestrator
    // can deserialize the inventory response event.
    PURCHASED, REJECTED, CANCELLED;
}
