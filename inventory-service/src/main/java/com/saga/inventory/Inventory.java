package com.saga.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Inventory extends JpaRepository<Item, Item.ItemId> {

}
