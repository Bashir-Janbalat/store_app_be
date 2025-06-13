package org.store.app.service;

import org.store.app.dto.CartDTO;
import org.store.app.enums.CartStatus;
import org.store.app.model.Cart;

import java.math.BigDecimal;

public interface CartService {
    CartDTO getActiveCart(String email, String sessionId);

    void addToCart(String email, String sessionId, Long productId, BigDecimal unitPrice, int quantity);

    void updateCartItemQuantity(String email, String sessionId, Long productId, int newQuantity);

    void removeFromCart(String email, String sessionId, Long productId);

    void clearCart(String email, String sessionId);

    void mergeCartOnLogin(String email, String sessionId);

    Cart getCartById(Long cartId, CartStatus status, Long customerId);
}
