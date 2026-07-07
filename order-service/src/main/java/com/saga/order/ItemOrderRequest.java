package com.saga.order;

import lombok.Builder;

@Builder
public record ItemOrderRequest(Long itemId,
                           Long quantity,
                           Long shopperId,
                           Long paymentDue,
                           String creditCardNum) {
    Order toOrder() {
        return Order.builder()
        .itemId(itemId)
        .quantity(quantity)
        .shopperId(shopperId)
        .paymentDue(paymentDue)
        .creditCardNum(creditCardNum)
        .build();
    }

}
