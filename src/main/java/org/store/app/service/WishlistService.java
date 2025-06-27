package org.store.app.service;

import org.store.app.common.ValueWrapper;
import org.store.app.dto.WishlistItemDTO;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;

public interface WishlistService {

    ValueWrapper<List<WishlistItemDTO>> getWishlistItemsForCurrentCustomer(String email, String sessionId);

    void addToWishlist(String email, String sessionId, Long productId) throws AccessDeniedException;

    void removeFromWishlist(String email, String sessionId, Long productId);

    void clearWishlist(String email, String sessionId);

    void mergeWishlistOnLogin(String email, String sessionId);

    int deleteOldAnonymousWishlists(LocalDateTime cutoffDate);
}
