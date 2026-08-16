package com.saga.order.web;

import com.saga.order.ItemOrderUseCase;
import com.saga.order.Orders;
import com.saga.order.ItemOrderRequest;
import com.saga.order.Order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import lombok.Builder;
import lombok.RequiredArgsConstructor;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

@RestController
@RequestMapping(path = "/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final ItemOrderUseCase itemOrderUseCase;
    private final Orders orders;

    @Builder
    record OrderRecord(UUID orderId,
                       Long itemId,
                       Long quantity,
                       Long shopperId,
                       Order.Status status) {}
    
    record ApiError(String message){}

    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    ResponseEntity<?> recordOrder(@RequestBody ItemOrderRequest request) {
        var order = itemOrderUseCase.makeOrder(request);
        //returns a 202 with an empty body and the order's generated id in the location header
        //status is 202 and not 201 because the id is created immediately but the outcome isn't, as payment-service and inventory-service handle processing asynchronously
        return ResponseEntity.accepted()
                .location(fromCurrentRequest().path("/{id}").build(order.getOrderId().toString()))
                .header(HttpHeaders.RETRY_AFTER, "2") //"2" matches FixedBackOff(1000L, 2) in KafkaConfig.java's DLT processing
                .build();
    }

    /**
     * Polls orders table given an orderId
     * Used by consumers to learn the status of an order (PENDING, SUCCEED, FAILED)
     * @param orderId primary key on the orders table
     * @return if the primary key exists returns a 200 and the row associated with the primary key, 404 ApiError otherwise
     */
    @GetMapping(path="/{orderId}", produces = APPLICATION_JSON_VALUE)
    ResponseEntity<?> status(@PathVariable UUID orderId) {
        var order = orders.findById(new Order.OrderId(orderId));

        if (order.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("Order Not Found"));
        }

        return ResponseEntity.ok().body(OrderRecord.builder()
                                        .orderId(order.get().getOrderId().id())
                                        .itemId(order.get().getItemId())
                                        .quantity(order.get().getQuantity())
                                        .shopperId(order.get().getShopperId())
                                        .status(order.get().getStatus())
                                        .build());
    }
}
