package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.common.ValueWrapper;
import org.store.app.dto.ProductInfoDTO;
import org.store.app.dto.WishlistItemDTO;
import org.store.app.enums.WishlistStatus;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.model.Customer;
import org.store.app.model.Wishlist;
import org.store.app.model.WishlistItem;
import org.store.app.projection.WishlistItemProductProjection;
import org.store.app.repository.CustomerRepository;
import org.store.app.repository.WishlistItemRepository;
import org.store.app.repository.WishlistRepository;
import org.store.app.service.WishlistService;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    @Caching(cacheable = {
            @Cacheable(value = "wishlistItems", key = "'session:' + #sessionId", condition = "#sessionId != null && #sessionId.length() > 0")
    })
    public ValueWrapper<List<WishlistItemDTO>> getWishlistItemsForCurrentCustomer(String email, String sessionId) {
        Wishlist wishlist = findActiveWishlistOrNull(email, sessionId);

        if (wishlist == null && email != null && !email.isBlank()) {
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            Wishlist newWishlist = new Wishlist();
            newWishlist.setStatus(WishlistStatus.ACTIVE);
            newWishlist.setCustomer(customer);
            newWishlist.setSessionId(sessionId);
            wishlistRepository.save(newWishlist);
            return new ValueWrapper<>(Collections.emptyList());
        }
        if (wishlist == null && sessionId != null && !sessionId.isBlank()) {
            Wishlist newWishlist = new Wishlist();
            newWishlist.setStatus(WishlistStatus.ACTIVE);
            newWishlist.setSessionId(sessionId);
            wishlistRepository.save(newWishlist);
            return new ValueWrapper<>(Collections.emptyList());
        }

        List<WishlistItemProductProjection> projections = wishlistItemRepository.findWishlistItemsWithProductInfo(wishlist.getId());

        List<WishlistItemDTO> result = projections.stream()
                .map(p -> new WishlistItemDTO(
                        p.getProductId(),
                        p.getUnitPrice(),
                        new ProductInfoDTO(p.getName(), p.getDescription(), p.getImageUrl(), p.getTotalStock())
                ))
                .collect(Collectors.toList());

        log.info("Loaded {} wishlist items from database for wishlistId={} (set in cache with Key='session:{}')", result.size(), wishlist.getId(), sessionId);
        return new ValueWrapper<>(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = "wishlistItems", key = "'session:' + #sessionId")
    })
    public void addToWishlist(String email, String sessionId, Long productId) throws AccessDeniedException {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID must not be null");
        }

        Wishlist wishlist = findActiveWishlistOrNull(email, sessionId);
        if (wishlist == null) {
            throw new AccessDeniedException("Unauthorized access");
        }

        boolean exists = wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId);
        if (!exists) {
            WishlistItem item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProductId(productId);
            wishlistItemRepository.save(item);
            log.info("Added product id {} to wishlist (key: {})", productId, email != null ? "email:" + email : "session:" + sessionId);
        } else {
            log.info("Product with id {} already exists in wishlist", productId);
        }
        logCacheEvict(sessionId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "wishlistItems", key = "'session:' + #sessionId")
    })
    public void removeFromWishlist(String email, String sessionId, Long productId) {
        Wishlist wishlist = findActiveWishlist(email, sessionId);
        WishlistItem item = wishlistItemRepository.findByWishlistIdAndProductId(wishlist.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));
        wishlistItemRepository.delete(item);
        log.info("Removed product id {} from wishlist (key: {})", productId, email != null ? "email:" + email : "session:" + sessionId);
        logCacheEvict(sessionId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "wishlistItems", key = "'session:' + #sessionId")
    })
    public void clearWishlist(String email, String sessionId) {
        Wishlist wishlist = findActiveWishlistOrNull(email, sessionId);
        if (wishlist == null) {
            return;
        }
        wishlistItemRepository.deleteAllByWishlistId(wishlist.getId());
        log.info("Clearing wishlist for '{}'", email != null ? email : sessionId);
        logCacheEvict(sessionId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "wishlistItems", key = "'session:' + #sessionId")
    })
    public void mergeWishlistOnLogin(String email, String sessionId) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with email: " + email));

        Optional<Wishlist> userWishlistOpt = wishlistRepository.findByCustomerAndStatus(customer, WishlistStatus.ACTIVE);
        Optional<Wishlist> sessionWishlistOpt = wishlistRepository.findBySessionIdAndStatus(sessionId, WishlistStatus.ACTIVE);

        if (sessionWishlistOpt.isEmpty()) return;

        Wishlist sessionWishlist = sessionWishlistOpt.get();

        if (userWishlistOpt.isEmpty()) {
            sessionWishlist.setCustomer(customer);
            wishlistRepository.save(sessionWishlist);
        } else {
            Wishlist userWishlist = userWishlistOpt.get();

            List<WishlistItem> sessionItems = wishlistItemRepository.findByWishlist(sessionWishlist);
            Set<Long> existingProductIds = wishlistItemRepository.findByWishlist(userWishlist).stream()
                    .map(WishlistItem::getProductId)
                    .collect(Collectors.toSet());

            for (WishlistItem item : sessionItems) {
                if (!existingProductIds.contains(item.getProductId())) {
                    WishlistItem newItem = new WishlistItem();
                    newItem.setWishlist(userWishlist);
                    newItem.setProductId(item.getProductId());
                    wishlistItemRepository.save(newItem);
                }
                wishlistItemRepository.delete(item);
            }
            // أبقاء السيشن في حال أنتهت صالحية التوكن
            userWishlist.setSessionId(sessionWishlist.getSessionId());
            wishlistRepository.save(userWishlist);
            if (wishlistItemRepository.findByWishlist(sessionWishlist).isEmpty()) {
                wishlistRepository.delete(sessionWishlist);
            }
        }
        log.info("Merged wishlist for '{}' and '{}'", email, sessionId);
        logCacheEvict(sessionId);
    }

    @Override
    public int deleteOldAnonymousWishlists(LocalDateTime cutoffDate) {
        return wishlistRepository.deleteWishlistsWithoutCustomerBefore(cutoffDate);
    }

    private Wishlist findActiveWishlistOrNull(String email, String sessionId) {
        return customerRepository.findByEmail(email)
                .flatMap(customerEntity -> wishlistRepository.findByCustomerAndStatus(customerEntity, WishlistStatus.ACTIVE))
                .orElseGet(() -> {
                    if (sessionId != null && !sessionId.isBlank()) {
                        return wishlistRepository.findBySessionIdAndStatus(sessionId, WishlistStatus.ACTIVE).orElse(null);
                    }
                    return null;
                });
    }

    private Wishlist findActiveWishlist(String email, String sessionId) {
        Wishlist wishlist = findActiveWishlistOrNull(email, sessionId);
        if (wishlist == null) {
            throw new ResourceNotFoundException("Wishlist not found");
        }
        return wishlist;
    }

    private void logCacheEvict(String sessionId) {
        log.info("Cache 'wishlistItems' evicted for key: session='{}'", sessionId);
    }
}
