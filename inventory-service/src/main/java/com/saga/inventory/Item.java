package com.saga.inventory;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "inventory")
@NoArgsConstructor(access = PRIVATE, force = true)
public class Item {

    @Embeddable
    public record ItemId(Integer id) implements Serializable {
        public String toString() {
            return id.toString();
        }
    }

    @Id
    private ItemId id;

    private final Instant creationTime;

    private String name;
    private Float price;
    private Integer stockAmount;

    public Item(ItemId id, String name, Float price, Integer stockAmount) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stockAmount = stockAmount;
        this.creationTime = Instant.now();
    }

    public boolean isAvailableInQuantity(int quantity) {
        return this.stockAmount >= quantity;
    }

    public void reserveItem(int quantity) {
        this.stockAmount -= quantity;
    }

    public void releaseItem(int quantity) {
        this.stockAmount += quantity;
    }

    public float calculatePrice(int quantity) {
        return quantity * this.price;
    }
}
