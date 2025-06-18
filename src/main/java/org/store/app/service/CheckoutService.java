package org.store.app.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import org.store.app.security.userdetails.CustomUserDetails;

public interface CheckoutService {

    Session createCheckoutSession(Long orderId, String currency, CustomUserDetails userDetails) throws StripeException;
}
