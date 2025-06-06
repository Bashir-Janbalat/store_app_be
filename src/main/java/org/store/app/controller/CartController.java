package org.store.app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.store.app.dto.AddToCartRequest;
import org.store.app.dto.CartItemDTO;
import org.store.app.dto.UpdateCartRequest;
import org.store.app.service.CartService;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public ResponseEntity<List<CartItemDTO>> getCartItems(@RequestParam(required = false) String sessionId) {
        String email = getCurrentUserEmail();
        List<CartItemDTO> items = cartService.getCartItemsForCurrentCustomer(email, sessionId);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(@RequestBody AddToCartRequest request) {
        String email = getCurrentUserEmail();
        cartService.addToCart(email, request.getSessionId(), request.getProductId(), request.getUnitPrice(), request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/update-quantity")
    public ResponseEntity<Void> updateQuantity(@RequestBody UpdateCartRequest request) {
        String email = getCurrentUserEmail();
        cartService.updateCartItemQuantity(email, request.getSessionId(), request.getProductId(), request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeFromCart(@RequestParam Long productId,
                                               @RequestParam(required = false) String sessionId) {
        String email = getCurrentUserEmail();
        cartService.removeFromCart(email, sessionId, productId);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }
}
