package com.saga.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.UUID;

import lombok.NoArgsConstructor;
import lombok.ToString;

import static lombok.AccessLevel.PRIVATE;

/**
 * Payment contains the metadata and relevant business information for a payment
 */
@Entity
@Table(name="payment")
@NoArgsConstructor(access = PRIVATE, force = true)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Payment {
    /**
     * Although purchaseId, shopperId, paymentAmount, creditCardNum, and type do not change once initialized, we cannot use the keyword "final"
     * This is due to Jackson 3's deserialization first initializing to null, and "final" prevents the actual information from being deserialized
     */ 
    @Id
    public UUID purchaseId;

    public Integer shopperId;

    public Long paymentAmount;

    public String creditCardNum;

    @Enumerated(EnumType.STRING)
    public PaymentRequestType type;

    /**
     * Computes PaymentStatus to demo saga implementation
     * @return REQUESTED (happy path), FAILED (unhappy path), CANCELLED (compensating transaction)
     */
    public PaymentStatus paymentStatus() {
        if (type == null || creditCardNum == null) {
            return PaymentStatus.FAILED;
        }

        PaymentStatus status;
        if (type.isRequest()) {
            if (creditCardNum.endsWith("1234")) { //deliberate failure mode for demo purposes
                status = PaymentStatus.FAILED;
            }
            else {
                status = PaymentStatus.REQUESTED;
            }
        }
        else {
            status = PaymentStatus.CANCELLED;
        }
        return status;
    }

}
