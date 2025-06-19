package org.store.app.service;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;

public interface StripeEventHandlerService {

    boolean canHandle(String eventType);

    void handle(Event event);

    default Session extractSession(Event event) {
        return (Session) event.getDataObjectDeserializer().getObject().orElse(null);
    }

    default StripeSessionData extractSessionData(Session session) {
        return new StripeSessionData(
                Long.valueOf(session.getClientReferenceId()),
                Long.valueOf(session.getMetadata().get("customer_id")),
                Long.valueOf(session.getMetadata().get("payment_id")),
                session.getPaymentIntent()
        );
    }

    record StripeSessionData(Long orderId, Long customerId, Long paymentId, String paymentIntent) {
    }
}
