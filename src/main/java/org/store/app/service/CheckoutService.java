package org.store.app.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

public interface CheckoutService {

    Session createCheckoutSession(Long orderId, String currency) throws StripeException;
}
