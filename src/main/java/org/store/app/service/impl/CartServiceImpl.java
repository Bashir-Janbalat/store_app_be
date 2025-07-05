package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.dto.CartDTO;
import org.store.app.enums.CartStatus;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.mapper.CartMapper;
import org.store.app.model.Cart;
import org.store.app.model.CartItem;
import org.store.app.model.Customer;
import org.store.app.projection.CartItemProductProjection;
import org.store.app.repository.CartItemRepository;
import org.store.app.repository.CartRepository;
import org.store.app.repository.CustomerRepository;
import org.store.app.service.CartService;
import org.store.app.service.InventoryQueryService;

import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final InventoryQueryService inventoryQueryService;
    private final CartMapper cartMapper;

    @Override
    @Transactional
    @Cacheable(value = "cart", key = "'session:' + #sessionId", condition = "#sessionId != null && #sessionId.length() > 0")
    public CartDTO getActiveCart(String email, String sessionId) {
        Cart cart = findActiveCartOrNull(email, sessionId);

        if (cart == null && email != null && !email.isBlank()) {
            log.info("No active cart found for email: '{}'", email);
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            cart = new Cart();
            cart.setStatus(CartStatus.ACTIVE);
            cart.setCustomer(customer);
            cart.setSessionId(sessionId);
            Cart saved = cartRepository.save(cart);
            return cartMapper.toDto(saved);
        }


        if (cart == null && sessionId != null && !sessionId.isBlank()) {
            log.info("No active cart found for cacheKey: '{}' and email: '{}'", sessionId, email);
            cart = new Cart();
            cart.setStatus(CartStatus.ACTIVE);
            cart.setSessionId(sessionId);
            Cart saved = cartRepository.save(cart);
            return cartMapper.toDto(saved);
        }

        List<CartItemProductProjection> projections = cartItemRepository.findCartItemsWithProductInfo(cart.getId());

        CartDTO cartDTO = cartMapper.toDtoFromProjections(cart.getId(), projections);
        log.info("Loaded {} cart items from database for cartId={} (set in cache with Key='session:{}')", projections.size(), cart.getId(), sessionId);
        return cartDTO;
    }


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "cart", key = "'session:' + #sessionId")})
    public void addToCart(String email, String sessionId, Long productId, BigDecimal unitPrice, int quantity) throws AccessDeniedException {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Cart cart = findActiveCartOrNull(email, sessionId);

        if (cart == null) {
            throw new AccessDeniedException("Unauthorized access");
        }
        int availableStock = inventoryQueryService.getAvailableStock(productId);
        int existingQuantity = cartItemRepository.findQuantityByCartIdAndProductId(cart.getId(), productId).orElse(0);
        int requestedTotalQuantity = existingQuantity + quantity;

        if (requestedTotalQuantity > availableStock) {
            throw new IllegalArgumentException("Out of stock");
        }

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).map(existingItem -> {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            log.info("Updated existing cart item: productId={}, newQuantity={}", productId, existingItem.getQuantity());
            return existingItem;
        }).orElseGet(() -> {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(productId);
            newItem.setUnitPrice(unitPrice); //Todo::  تأكد من أنك تثق بهذا السعر أو حمّله من قاعدة البيانات
            newItem.setQuantity(quantity);
            log.info("Added new cart item: productId={}, quantity={}", productId, quantity);
            return newItem;
        });

        cartItemRepository.save(item);
        logCacheEvict(sessionId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "cart", key = "'session:' + #sessionId")})
    public void updateCartItemQuantity(String email, String sessionId, Long productId, int newQuantity) {
        String identifier = email != null ? email : sessionId;
        log.info("Updating quantity for productId={} in cart for '{}'. New quantity: {}", productId, identifier, newQuantity);

        if (newQuantity <= 0) {
            removeFromCart(email, sessionId, productId);
            return;
        }

        Cart cart = findActiveCart(email, sessionId);

        int availableStock = inventoryQueryService.getAvailableStock(productId);
        if (newQuantity > availableStock) {
            throw new IllegalArgumentException("Out of stock");
        }

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
        log.info("Updated productId={} to quantity={}", productId, newQuantity);

        logCacheEvict(sessionId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "cart", key = "'session:' + #sessionId")})
    public void removeFromCart(String email, String sessionId, Long productId) {
        String identifier = email != null ? email : sessionId;
        log.info("Attempting to remove productId={} from cart for '{}'", productId, identifier);
        Cart cart = findActiveCart(email, sessionId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cartItemRepository.delete(item);
        log.info("Removed productId={} from cart for '{}'", productId, identifier);
        logCacheEvict(sessionId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "cart", key = "'session:' + #sessionId")})
    public void clearCart(String email, String sessionId) {
        String identifier = email != null ? email : sessionId;

        Cart cart = findActiveCartOrNull(email, sessionId);
        if (cart == null) {
            log.info("No active cart found for '{}'. Nothing to clear.", identifier);
            return;
        }

        int count = cart.getItems().size();
        cart.getItems().clear();
        log.info("Cleared {} item(s) from cart for '{}'", count, identifier);
        logCacheEvict(sessionId);
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "cart", key = "'session:' + #sessionId")})
    public void mergeCartOnLogin(String email, String sessionId) {
        Customer customer = customerRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Optional<Cart> userCartOpt = cartRepository.findByCustomerAndStatus(customer, CartStatus.ACTIVE);
        Optional<Cart> sessionCartOpt = cartRepository.findBySessionIdAndStatus(sessionId, CartStatus.ACTIVE);

        if (sessionCartOpt.isEmpty()) {
            // لا يوجد سلة مؤقتة، لا شيء لدمجه
            return;
        }

        Cart sessionCart = sessionCartOpt.get();

        if (userCartOpt.isEmpty()) {
            // ليس لدى المستخدم سلة، قم بتحويل السلة المؤقتة إلى سلة المستخدم
            sessionCart.setCustomer(customer);
            cartRepository.save(sessionCart);
        } else {
            Cart userCart = userCartOpt.get();

            // دمج عناصر السلة
            List<CartItem> sessionItems = cartItemRepository.findByCart(sessionCart);
            List<CartItem> userItems = cartItemRepository.findByCart(userCart);

            Map<Long, CartItem> userItemsMap = userItems.stream().collect(Collectors.toMap(CartItem::getProductId, item -> item));
            List<CartItem> itemsToDelete = new ArrayList<>();
            for (CartItem sessionItem : sessionItems) {
                CartItem userItem = userItemsMap.get(sessionItem.getProductId());
                int availableStock = inventoryQueryService.getAvailableStock(sessionItem.getProductId());
                int desiredQuantity = sessionItem.getQuantity();
                if (userItem != null) {
                    desiredQuantity += userItem.getQuantity();
                }
                if (desiredQuantity > availableStock) {
                    log.warn("Insufficient stock for productId={}, requested={}, available={}, email={}",
                            sessionItem.getProductId(), desiredQuantity, availableStock, email);
                }
                int finalQuantity = Math.min(desiredQuantity, availableStock);
                if (userItem != null) {
                    // دمج الكميات
                    userItem.setQuantity(finalQuantity);
                    cartItemRepository.save(userItem);
                    itemsToDelete.add(sessionItem);
                } else {
                    // إنشاء عنصر جديد للسلة الجديدة
                    CartItem newItem = new CartItem();
                    newItem.setCart(userCart);
                    newItem.setProductId(sessionItem.getProductId());
                    newItem.setQuantity(finalQuantity);
                    newItem.setUnitPrice(sessionItem.getUnitPrice());
                    cartItemRepository.save(newItem);

                    // حذف العنصر من السلة المؤقتة
                    itemsToDelete.add(sessionItem);
                }
            }
            // أبقاء السيشن في حال أنتهت صالحية التوكن
            userCart.setSessionId(sessionCart.getSessionId());
            cartRepository.save(userCart);
            cartItemRepository.deleteAll(itemsToDelete);
            cartItemRepository.flush();
            // حذف السلة المؤقتة إذا فارغة الآن
            if (cartItemRepository.findByCart(sessionCart).isEmpty()) {
                cartRepository.delete(sessionCart);
            }
        }
        log.info("Merged session cart into customer cart for email='{}', sessionId='{}'", email, sessionId);
        logCacheEvict(sessionId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "cart", allEntries = true)})
    public void updateCartStatus(Long cartId, CartStatus newStatus) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

        cart.setStatus(newStatus);
        cartRepository.save(cart);
        log.info("Updated cart status to {} for cartId={}", newStatus, cartId);
        log.info("Cache entries evicted: 'cart' (allEntries)");
    }

    @Override
    public int deleteOldAnonymousCarts(LocalDateTime cutoffDate) {
        return cartRepository.deleteCartsWithoutCustomerBefore(cutoffDate);
    }


    private Cart findActiveCartOrNull(String email, String sessionId) {
        if (email != null && !email.isBlank()) {
            Optional<Customer> customerOpt = customerRepository.findByEmail(email);
            if (customerOpt.isPresent()) {
                Optional<Cart> cartOpt = cartRepository.findByCustomerAndStatus(customerOpt.get(), CartStatus.ACTIVE);
                if (cartOpt.isPresent()) return cartOpt.get();
            }
        }

        if (sessionId != null && !sessionId.isBlank()) {
            return cartRepository.findBySessionIdAndStatus(sessionId, CartStatus.ACTIVE).orElse(null);
        }

        return null;
    }


    private Cart findActiveCart(String email, String sessionId) {
        Cart cart = findActiveCartOrNull(email, sessionId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart not found for given email/sessionId");
        }
        return cart;
    }

    private void logCacheEvict(String sessionId) {
        log.info("Cache 'cart' evicted for key session='{}'", sessionId);
    }
}
