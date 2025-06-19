package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.enums.PaymentStatus;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.model.Order;
import org.store.app.model.Payment;
import org.store.app.repository.PaymentRepository;
import org.store.app.service.PaymentService;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional
    @Override
    public Payment createPendingPayment(Order order, BigDecimal amount, String method) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Created pending payment with ID {} for Order ID {}, amount {}, method {}", savedPayment.getId(), order.getId(), amount, method);
        return savedPayment;
    }

    @Transactional
    @Override
    public void updatePaymentStatus(Long paymentId, PaymentStatus status, String transactionId, String responseMessage) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        payment.setStatus(status);
        payment.setTransactionId(transactionId);
        payment.setResponseMessage(responseMessage);

        paymentRepository.save(payment);
        log.info("Updated payment ID {} status to {}, transactionId: {}, responseMessage: {}", paymentId, status, transactionId, responseMessage);
    }
}