package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;


    public List<CartItemDTO> getCartItemsForCurrentCustomer(String email, String sessionId) {
        Customer customer = null;
        if (email != null && !email.isEmpty()) {
            customer = customerRepository.findByEmail(email).orElse(null);
        }

        Optional<Cart> cart;

        if (customer != null) {
            cart = cartRepository.findByCustomerAndStatus(customer, CartStatus.ACTIVE);
        } else if (sessionId != null && !sessionId.isEmpty()) {
            cart = cartRepository.findBySessionIdAndStatus(sessionId, CartStatus.ACTIVE);
        } else {
            return Collections.emptyList();
        }

        if (cart.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<CartItemProductProjection> projections = cartItemRepository.findCartItemsWithProductInfo(cart.get().getId());

            return projections.stream()
                    .map(p -> new CartItemDTO(
                            p.getProductId(),
                            p.getQuantity(),
                            p.getUnitPrice(),
                            new ProductInfoDTO(p.getName(), p.getDescription(), p.getImageUrl())
                    ))
                    .toList();
        }
    }

    @Transactional
    public void addToCart(String email, String sessionId, Long productId, BigDecimal unitPrice, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Customer customer = null;
        if (email != null && !email.isEmpty()) {
            customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer with email {" + email + "} not found"));
        }

        Cart cart = null;

        if (customer != null) {
            cart = cartRepository.findByCustomerAndStatus(customer, CartStatus.ACTIVE).orElse(null);
        }
        if (cart == null && sessionId != null && !sessionId.isEmpty()) {
            cart = cartRepository.findBySessionIdAndStatus(sessionId, CartStatus.ACTIVE).orElse(null);
        }

        if (cart == null) {
            cart = new Cart();
            cart.setCustomer(customer);
            cart.setSessionId(sessionId);
            cart.setStatus(CartStatus.ACTIVE);
            cart = cartRepository.save(cart);
        }

        Cart finalCart = cart;
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .map(existingItem -> {
                    existingItem.setQuantity(existingItem.getQuantity() + quantity);
                    return existingItem;
                })
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(finalCart);
                    newItem.setProductId(productId);
                    newItem.setUnitPrice(unitPrice);
                    newItem.setQuantity(quantity);
                    return newItem;
                });

        cartItemRepository.save(item);
    }

    @Transactional
    public void updateCartItemQuantity(String email, String sessionId, Long productId, int newQuantity) {
        Customer customer = null;
        if (email != null && !email.isEmpty()) {
            customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        }

        Cart cart;
        if (customer != null) {
            cart = cartRepository.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        } else if (sessionId != null && !sessionId.isEmpty()) {
            cart = cartRepository.findBySessionIdAndStatus(sessionId, CartStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        } else {
            throw new ResourceNotFoundException("Cart not found");
        }

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        if (newQuantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(newQuantity);
            cartItemRepository.save(item);
        }
    }

    @Transactional
    public void removeFromCart(String email, String sessionId, Long productId) {
        Customer customer = null;
        if (email != null && !email.isEmpty()) {
            customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        }

        Cart cart;
        if (customer != null) {
            cart = cartRepository.findByCustomerAndStatus(customer, CartStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        } else if (sessionId != null && !sessionId.isEmpty()) {
            cart = cartRepository.findBySessionIdAndStatus(sessionId, CartStatus.ACTIVE)
                    .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        } else {
            throw new ResourceNotFoundException("Cart not found");
        }

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cartItemRepository.delete(item);
    }
}
