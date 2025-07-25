package org.store.app.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.store.app.common.StripeEventDispatcher;
import org.store.app.security.userdetails.CustomUserDetails;
import org.store.app.service.CheckoutService;

@Slf4j
@RestController
@RequestMapping("/store/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final StripeEventDispatcher stripeEventDispatcher;

    public static final String CURRENCY = "EUR";

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/create-session")
    public ResponseEntity<?> createCheckoutSession(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
                                                   @RequestParam Long orderId) {
        if (orderId == null) {
            return ResponseEntity.badRequest().body("Order ID is required");
        }
        try {
            Session session = checkoutService.createCheckoutSession(orderId, CURRENCY, userDetails);
            return ResponseEntity.ok(session.getUrl());
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Validation error while creating checkout session: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (StripeException e) {
            log.error("StripeException while creating checkout session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Stripe error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while creating checkout session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
        }
    }


    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("Received webhook: {}", event.getType());

            stripeEventDispatcher.dispatch(event);

            return ResponseEntity.ok("Webhook handled: " + event.getType());

        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Unexpected error in Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal error");
        }
    }
}
