package com.saga.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.NoArgsConstructor;
import static lombok.AccessLevel.PRIVATE;

import java.io.Serializable;
import java.util.UUID;
import java.time.Instant;
import static java.util.Objects.requireNonNull;

/**
 * Outbox is an Entity class representing an Outbox Event row for storing persistently
 * 
 */
@Entity
// see EventLog.java in inventory-service for why this must be explicit and lowercase
@Table(name = "outboxevent")
@NoArgsConstructor(access = PRIVATE)
public class Outbox implements Serializable {

    @Id
    private UUID id;
    private Instant timestamp;
    
    @Column(name = "aggregateid")
    private String aggregateId;

    @Column(name = "aggregatetype")
    private String aggregateType;
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    private Object payload;

    Outbox(OutboxEvent<?,?> event) {
        requireNonNull(event, "event cannot be null.");
        this.id = UUID.randomUUID();
        this.timestamp = requireNonNull(event.getTimestamp(), "timestamp cannot be null.");
        this.aggregateId = requireNonNull(event.getAggregateId(), "aggregateId cannot be null.").toString();
        this.aggregateType = requireNonNull(event.getAggregateType(), "aggregateType cannot be null.");
        this.type = requireNonNull(event.getType(), "type cannot be null");
        this.payload = requireNonNull(event.getPayload(), "payload cannot be null");
    }
}
