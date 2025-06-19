package org.store.app.service;

import org.store.app.enums.PaymentStatus;
import org.store.app.model.Order;
import org.store.app.model.Payment;

import java.math.BigDecimal;

public interface PaymentService {

    Payment createPendingPayment(Order order, BigDecimal amount, String method);
    void updatePaymentStatus(Long paymentId, PaymentStatus status, String transactionId, String responseMessage);
}
