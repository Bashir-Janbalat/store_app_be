package org.store.app.service;

import org.store.app.dto.CartDTO;
import org.store.app.enums.CartStatus;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

public interface CartService {
    CartDTO getActiveCart(String email, String sessionId);

    void addToCart(String email, String sessionId, Long productId, BigDecimal unitPrice, int quantity) throws AccessDeniedException;

    void updateCartItemQuantity(String email, String sessionId, Long productId, int newQuantity);

    void removeFromCart(String email, String sessionId, Long productId);

    void clearCart(String email, String sessionId);

    void mergeCartOnLogin(String email, String sessionId);

    void updateCartStatus(Long cartId, CartStatus newStatus);

    int deleteOldAnonymousCarts(LocalDateTime cutoffDate);
}
