package com.saga.order;

import org.springframework.stereotype.Repository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

@Repository
public interface Orders extends CrudRepository<Order, Order.OrderId> {

    @Query("""
            SELECT o.status
            FROM Order r
            WHERE r.id = :id
            """)
    Optional<Order.Status> queryOrderStatus(Order.OrderId id);
}
