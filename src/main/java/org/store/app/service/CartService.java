package org.store.app.service;

import org.store.app.dto.CartItemDTO;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {
    List<CartItemDTO> getCartItemsForCurrentCustomer(String email, String sessionId);

    void addToCart(String email, String sessionId, Long productId, BigDecimal unitPrice, int quantity);

    void updateCartItemQuantity(String email, String sessionId, Long productId, int newQuantity);

    void removeFromCart(String email, String sessionId, Long productId);

    void clearCart(String email, String sessionId);

    void mergeCartOnLogin(String email, String sessionId);
}
