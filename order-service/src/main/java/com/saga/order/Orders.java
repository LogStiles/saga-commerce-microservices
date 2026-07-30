package com.saga.order;

import org.springframework.stereotype.Repository;
import org.springframework.data.repository.CrudRepository;

@Repository
public interface Orders extends CrudRepository<Order, Order.OrderId> {
}
