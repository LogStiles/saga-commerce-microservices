package com.saga.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.UUID;

import lombok.NoArgsConstructor;
import lombok.ToString;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name="payment")
@NoArgsConstructor(access = PRIVATE, force = true)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Payment {
    @Id
    public UUID purchaseId;

    public Integer shopperId;

    public Long paymentAmount;

    public String creditCardNum;

    @Enumerated(EnumType.STRING)
    public PaymentRequestType type;

    public PaymentStatus paymentStatus() {
        if (type == null || creditCardNum == null) {
            return PaymentStatus.FAILED;
        }

        PaymentStatus status;
        if (type.isRequest()) {
            if (creditCardNum.endsWith("1234")) {
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
