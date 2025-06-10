package org.store.app.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.store.app.enums.PaymentStatus;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@EqualsAndHashCode(callSuper = true)
@Data
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    private String paymentMethod;
    private BigDecimal amount;
    private String transactionId;
    private String responseMessage;
}
