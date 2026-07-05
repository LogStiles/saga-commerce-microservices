package com.saga.payment;

import jakarta.persistence.*;

import java.util.UUID;

import lombok.NoArgsConstructor;
import lombok.ToString;

import static lombok.AccessLevel.PRIVATE;

@Entity
@Table(name="payment")
@NoArgsConstructor(access = PRIVATE, force = true)
@ToString
public class Payment {
    @Id
    public final UUID purchaseId;

    public final Integer shopperId;

    public final Long paymentAmount;

    public final String creditCardNum;

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
