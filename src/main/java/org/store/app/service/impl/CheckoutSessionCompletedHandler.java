package org.store.app.service.impl;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.store.app.enums.CartStatus;
import org.store.app.enums.OrderStatus;
import org.store.app.enums.PaymentStatus;
import org.store.app.model.Order;
import org.store.app.service.CartService;
import org.store.app.service.OrderService;
import org.store.app.service.PaymentService;
import org.store.app.service.StripeEventHandlerService;

@RequiredArgsConstructor
@Service
@Slf4j
public class CheckoutSessionCompletedHandler implements StripeEventHandlerService {

    private final OrderService orderService;
    private final CartService cartService;
    private final PaymentService paymentService;

    @Override
    public boolean canHandle(String eventType) {
        return "checkout.session.completed".equals(eventType);
    }

    @Override
    public void handle(Event event) {
        Session session = extractSession(event);
        if (session == null) return;

        StripeSessionData data = extractSessionData(session);

        Order order = orderService.updateOrderStatus(data.orderId(), OrderStatus.PROCESSING, data.customerId());
        paymentService.updatePaymentStatus(data.paymentId(), PaymentStatus.COMPLETED, data.paymentIntent(), "Payment completed successfully");
        orderService.sendOrderConfirmationEmail(data.orderId(), "EUR");
        cartService.updateCartStatus(order.getCart().getId(), CartStatus.CONVERTED);


        log.info("Handled checkout.session.completed for order: {}", data.orderId());
    }
}
