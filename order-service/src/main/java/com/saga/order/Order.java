package com.saga.order;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.NoArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.Serializable;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = PRIVATE, force = true)
public class Order {

    public enum Status {
        PENDING, SUCCEED, FAILED, CANCELED, REFUND
    }

    @Embeddable
    public record OrderId(UUID id) implements Serializable {
        public String toString() {
            return id.toString();
        }
    }

    @Id
    private final OrderId id;

    private Long itemId;
    private Long quantity;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Long shopperId;
    public Long paymentDue;
    public String creditCardNum;

    @Builder
    public Order(Long itemId,
                       Long quantity,
                       Long shopperId,
                       Long paymentDue,
                       String creditCardNum) {
        this.id = new OrderId(UUID.randomUUID());
        this.itemId = requireNonNull(itemId, "itemId cannot be null.");
        this.quantity = requireNonNull(quantity, "quantity cannot be null.");
        this.shopperId = requireNonNull(shopperId, "shopperId cannot be null.");
        this.paymentDue = requireNonNull(paymentDue, "paymentDue cannot be null.");
        this.creditCardNum = creditCardNum;
        this.status = Status.PENDING;
    }

    public OrderId getOrderId() {
        return this.id;
    }

    public Status getStatus() {
        return this.status;
    }

    public Long getItemId() {
        return this.itemId;
    }

    public Long getQuantity() {
        return this.quantity;
    }

    public Long getShopperId() {
        return this.shopperId;
    }

    public void markAsSucceeded() {
        this.status = Status.SUCCEED;
    }

    public void markAsFailed() {
        this.status = Status.FAILED;
    }

    public ObjectNode toTransactionPayload() {
        // Keys are consumed downstream by the participants: the payment-service
        // deserializes {purchaseId, shopperId, paymentAmount, creditCardNum, type}
        // into its Payment entity, and the inventory-service reads {itemId, quantity, type}.
        return new ObjectMapper().createObjectNode()
                    .put("orderId", this.id.toString())
                    .put("purchaseId", this.id.toString())
                    .put("itemId", this.itemId)
                    .put("quantity", this.quantity)
                    .put("shopperId", this.shopperId)
                    .put("paymentAmount", this.paymentDue)
                    .put("creditCardNum", creditCardNum);
    }
}
