package com.saga.payment;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface Payments extends CrudRepository<Payment, UUID> {
}
