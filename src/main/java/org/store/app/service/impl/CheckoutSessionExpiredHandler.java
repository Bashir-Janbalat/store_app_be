package org.store.app.service.impl;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.store.app.enums.OrderStatus;
import org.store.app.enums.PaymentStatus;
import org.store.app.service.OrderService;
import org.store.app.service.PaymentService;
import org.store.app.service.StripeEventHandlerService;

@RequiredArgsConstructor
@Service
@Slf4j
public class CheckoutSessionExpiredHandler implements StripeEventHandlerService {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @Override
    public boolean canHandle(String eventType) {
        return "checkout.session.expired".equals(eventType);
    }

    @Override
    public void handle(Event event) {
        Session session = extractSession(event);
        if (session == null) return;

        StripeSessionData data = extractSessionData(session);

        orderService.updateOrderStatus(data.orderId(), OrderStatus.CANCELLED, data.customerId());
        paymentService.updatePaymentStatus(data.paymentId(), PaymentStatus.FAILED, null, "Payment session expired");

        log.info("Handled checkout.session.expired for order: {}", data.orderId());
    }
}