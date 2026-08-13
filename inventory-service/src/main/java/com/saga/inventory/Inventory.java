package com.saga.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Inventory is a simple CRUD Repository that stores all relevant business information about items
 */
@Repository
public interface Inventory extends JpaRepository<Item, Item.ItemId> {

}
