package org.store.app.service.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.store.app.enums.OrderStatus;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.model.Order;
import org.store.app.repository.OrderRepository;
import org.store.app.service.CheckoutService;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutServiceImpl implements CheckoutService {

    @Value("${domain}")
    private String domain;

    private final OrderRepository orderRepository;

    @Override
    public Session createCheckoutSession(Long orderId, String currency) throws StripeException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new IllegalStateException("Order is not in a valid state for payment");
        }

        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order amount must be greater than zero");
        }

        long amount = totalAmount.multiply(BigDecimal.valueOf(100)).longValueExact();

        log.info("Creating Stripe session for Order ID: {}, Amount: {}, Currency: {}", orderId, totalAmount, currency);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(domain + "payment-success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(domain + "payment-cancel")
                .setClientReferenceId(orderId.toString())
                .setCustomerEmail(order.getCustomer().getEmail())
                .putMetadata("order_id", orderId.toString())
                .putMetadata("customer_id", order.getCustomer().getId().toString())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(amount)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Order #" + orderId)
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            return Session.create(params);
        } catch (StripeException e) {
            log.error("Stripe session creation failed for Order ID {}: {}", orderId, e.getMessage(), e);
            throw e;
        }
    }

}
