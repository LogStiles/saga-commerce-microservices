package com.saga.payment;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Payments is a simple CRUD Repository that stores all relevant business information about payments
 */
@Repository
public interface Payments extends CrudRepository<Payment, UUID> {
}
