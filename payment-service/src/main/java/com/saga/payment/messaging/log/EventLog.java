package com.saga.payment.messaging.log;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.Instant;

import static lombok.AccessLevel.PRIVATE;

@Entity
// see EventLog.java in inventory-service for why this must be explicit and lowercase
@Table(name = "eventlog")
@NoArgsConstructor(access = PRIVATE, force = true)
public class EventLog {
    @Id
    private final UUID eventId;

    private final Instant issuedOn;

    public EventLog(UUID eventId) {
        this.eventId = eventId;
        this.issuedOn = Instant.now();
    }
}
