package com.saga.inventory.messaging.log;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.time.Instant;

import static lombok.AccessLevel.PRIVATE;

@Entity
/**
 * Lowercase table names are the default for Spring and its automatically configured SpringPhysicalNamingStrategy, in addition to converting camelCase to snake_case 
 * (e.g. our EventLog would be event_log if we kept the class name for the table name).
 * By explicitly providing a table name in all lower case we can prevent Hibernate from looking for an event_log table that doesn't exist when we persist or query an EventLog.
 */
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
