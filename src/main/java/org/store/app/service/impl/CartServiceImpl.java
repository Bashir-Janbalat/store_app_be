package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.dto.CartDTO;
import org.store.app.dto.CartItemDTO;
import org.store.app.dto.ProductInfoDTO;
import org.store.app.enums.CartStatus;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.model.Cart;
import org.store.app.model.CartItem;
import org.store.app.model.Customer;
import org.store.app.projection.CartItemProductProjection;
import org.store.app.repository.CartItemRepository;
import org.store.app.repository.CartRepository;
import org.store.app.repository.CustomerRepository;
import org.store.app.service.CartService;

import java.math.BigDecimal;
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "cart", key = "#email != null ? #email : #sessionId")
    public CartDTO getActiveCart(String email, String sessionId) {
        String cacheKey = email != null ? email : sessionId;
        log.info("Fetching active cart for cacheKey: '{}'", cacheKey);
        Cart cart = findActiveCartOrNull(email, sessionId);

        if (cart == null) {
            log.info("No active cart found for cacheKey: '{}'", cacheKey);
            return new CartDTO();
        }

        List<CartItemProductProjection> projections = cartItemRepository.findCartItemsWithProductInfo(cart.getId());

        List<CartItemDTO> result = projections.stream().map(p -> new CartItemDTO(p.getProductId(), p.getQuantity(), p.getUnitPrice(),
                new ProductInfoDTO(p.getName(), p.getDescription(), p.getImageUrl(),p.getTotalStock()))).toList();
        log.info("Loaded {} cart items from database for cartId={} (key='{}')", result.size(), cart.getId(), cacheKey);
        return new CartDTO(cart.getId(), result);
    }


    @Override
    @Transactional
    @Caching(evict = {@CacheEvict(value = "cart", key = "#email != null ? #email : #sessionId"), @CacheEvict(value = "cartById")})
    public void addToCart(String email, String sessionId, Long productId, BigDecimal unitPrice, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Cart cart = findActiveCartOrNull(email, sessionId);

        if (cart == null) {
            cart = new Cart();
            cart.setStatus(CartStatus.ACTIVE);
            if (email != null && !email.isBlank()) {
                Customer customer = customerRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Customer with email {" + email + "} not found"));
                cart.setCustomer(customer);
                log.info("Created new cart for customer '{}'", email);
            } else {
                cart.setSessionId(sessionId);
                log.info("Created new cart for session '{}'", sessionId);
            }
            cart = cartRepository.save(cart);
        }

        Cart finalCart = cart;

        CartItem item = cartItemRepository.findByCartIdAndProductId(finalCart.getId(), productId).map(existingItem -> {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            log.info("Updated existing cart item: productId={}, newQuantity={}", productId, existingItem.getQuantity());
            return existingItem;
        }).orElseGet(() -> {
            CartItem newItem = new CartItem();
            newItem.setCart(finalCart);
            newItem.setProductId(productId);
            newItem.setUnitPrice(unitPrice); //Todo::  تأكد من أنك تثق بهذا السعر أو حمّله من قاعدة البيانات
            newItem.setQuantity(quantity);
            log.info("Added new cart item: productId={}, quantity={}", productId, quantity);
            return newItem;
        });

        cartItemRepository.save(item);
        log.info("Cart item saved. Evicted cache entries for 'cart' and 'cartById'");
    }

    @Override
    @Transactional
    @Caching(evict = {@CacheEvict(value = "cart", key = "#email != null ? #email : #sessionId"), @CacheEvict(value = "cartById")})
    public void updateCartItemQuantity(String email, String sessionId, Long productId, int newQuantity) {
        String identifier = email != null ? email : sessionId;
        log.info("Updating quantity for productId={} in cart for '{}'. New quantity: {}", productId, identifier, newQuantity);

        Cart cart = findActiveCart(email, sessionId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        if (newQuantity <= 0) {
            cartItemRepository.delete(item);
            log.info("Removed productId={} from cart (quantity <= 0)", productId);
        } else {
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
            log.info("Updated productId={} to quantity={}", productId, newQuantity);
        }
        log.info("Cache entries evicted: 'cart' and 'cartById'");
    }

    @Override
    @Transactional
    @Caching(evict = {@CacheEvict(value = "cart", key = "#email != null ? #email : #sessionId"), @CacheEvict(value = "cartById")})
    public void removeFromCart(String email, String sessionId, Long productId) {
        String identifier = email != null ? email : sessionId;
        log.info("Attempting to remove productId={} from cart for '{}'", productId, identifier);
        Cart cart = findActiveCart(email, sessionId);

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cartItemRepository.delete(item);
        log.info("Removed productId={} from cart for '{}'", productId, identifier);
        log.info("Cache entries evicted: 'cart' and 'cartById'");
    }

    @Override
    @Transactional
    @Caching(evict = {@CacheEvict(value = "cart", key = "#email != null ? #email : #sessionId"), @CacheEvict(value = "cartById")})
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
        log.info("Cache entries evicted: 'cart' and 'cartById'");
    }

    @Transactional
    @Override
    @Caching(evict = {@CacheEvict(value = "cart", key = "#email"), @CacheEvict(value = "cart", key = "#sessionId"), @CacheEvict(value = "cartById")})
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
            sessionCart.setSessionId(null);
            cartRepository.save(sessionCart);
        } else {
            Cart userCart = userCartOpt.get();

            // دمج عناصر السلة
            List<CartItem> sessionItems = cartItemRepository.findByCart(sessionCart);
            List<CartItem> userItems = cartItemRepository.findByCart(userCart);

            Map<Long, CartItem> userItemsMap = userItems.stream().collect(Collectors.toMap(CartItem::getProductId, item -> item));

            for (CartItem sessionItem : sessionItems) {
                CartItem userItem = userItemsMap.get(sessionItem.getProductId());
                if (userItem != null) {
                    // دمج الكميات
                    userItem.setQuantity(userItem.getQuantity() + sessionItem.getQuantity());
                    cartItemRepository.save(userItem);
                    cartItemRepository.delete(sessionItem);
                } else {
                    // إنشاء عنصر جديد للسلة الجديدة

                    CartItem newItem = new CartItem();
                    newItem.setCart(userCart);
                    newItem.setProductId(sessionItem.getProductId());
                    newItem.setQuantity(sessionItem.getQuantity());
                    newItem.setUnitPrice(sessionItem.getUnitPrice());
                    cartItemRepository.save(newItem);

                    // حذف العنصر من السلة المؤقتة
                    cartItemRepository.delete(sessionItem);
                }
            }
            // حذف السلة المؤقتة إذا فارغة الآن
            if (cartItemRepository.findByCart(sessionCart).isEmpty()) {
                cartRepository.delete(sessionCart);
            }
        }
        log.info("Merged session cart into customer cart for email='{}', sessionId='{}'", email, sessionId);
        log.info("Cache entries evicted: 'cart({})', 'cart({})', and all 'cartById'", email, sessionId);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "cart", allEntries = true),
            @CacheEvict(value = "cartById", allEntries = true)
    })
    public void updateCartStatus(Long cartId, CartStatus newStatus) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

        cart.setStatus(newStatus);
        cartRepository.save(cart);
        log.info("Updated cart status to {} for cartId={}", newStatus, cartId);
        log.info("Cache entries evicted: 'cart' and 'cartById'");
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
}
