package com.saga.order.framework;

import static lombok.AccessLevel.PRIVATE;

import static com.saga.order.framework.TransactionStepStatus.STARTED;
import static com.saga.order.framework.TransactionStepStatus.SUCCEEDED;
import static com.saga.order.framework.TransactionStepStatus.FAILED;
import static com.saga.order.framework.TransactionStepStatus.COMPENSATED;

import java.util.EnumSet;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.NoArgsConstructor;

/**
 * TransactionState holds the metadata of a multi-step TransactionSaga including a unique identifier, the location of the transaction in the state machine, and the health of the transaction
 */
@Entity
@Table(name = "transactionstate")
@NoArgsConstructor(access = PRIVATE, force = true)
public class TransactionState {
    @Id
    private UUID id;

    @Version
    private int version; //transactions change as the status and stepstatus update, version keeps track of them
    // Also serves as optimistic locking conflict detection, Hibernate will throw an OptimisticLockException if two writes to the same id are concurrent

    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;

    private String currentStep;

    @JdbcTypeCode(SqlTypes.JSON)
    private ObjectNode stepStatus;

    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;

    public TransactionState(String transactionType, JsonNode payload) {
        this.id = UUID.randomUUID();
        this.type = transactionType;
        this.payload = payload;
        this.transactionStatus = TransactionStatus.STARTED;
        this.stepStatus = JsonNodeFactory.instance.objectNode();
    }

    public UUID getId() {
        return this.id;
    }

    public JsonNode getPayload() {
        return this.payload;
    }

    public String getCurrentStep() {
        return this.currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public TransactionStatus getTransactionStatus() {
        return this.transactionStatus;
    }

    public void updateStepStatus(String step, TransactionStepStatus transactionStepStatus) {
        this.stepStatus.put(step, transactionStepStatus.name());
    }

    // Computes the status of the transaction as a whole by looking at the status of the transaction's steps
    // TransactionStatus should never be set by hand, the logic here works for all current and future situations and avoids introducing subtle failure points across the api
    public void advanceTransactionStatus() {
        var statuses = stepStatusToSet(); //convert all step statuses to a single set

        if (EnumSet.of(SUCCEEDED).containsAll(statuses)) {
            transactionStatus = TransactionStatus.FINISHED;      // every step succeeded
        } else if (EnumSet.of(STARTED, SUCCEEDED).containsAll(statuses)) {
            transactionStatus = TransactionStatus.STARTED;       // in flight, nothing failed
        } else if (EnumSet.of(FAILED, COMPENSATED).containsAll(statuses)) {
            transactionStatus = TransactionStatus.ABORTED;       // fully rolled back
        } else {
            transactionStatus = TransactionStatus.ABORTING;      // failure exists, still undoing
        }
    }

    private EnumSet<TransactionStepStatus> stepStatusToSet() {
        var allStatus = EnumSet.noneOf(TransactionStepStatus.class);
        stepStatus.valueStream()
                .map(node -> TransactionStepStatus.valueOf(node.asText()))
                .forEach(allStatus::add);
        return allStatus;
    }
}
