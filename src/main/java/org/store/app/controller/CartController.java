package org.store.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.store.app.common.ValueWrapper;
import org.store.app.dto.AddToCartRequest;
import org.store.app.dto.CartItemDTO;
import org.store.app.dto.UpdateCartRequest;
import org.store.app.service.CartService;

import java.util.List;

import static org.store.app.util.RequestUtils.getCurrentUserEmail;
import static org.store.app.util.RequestUtils.validateSessionOrEmail;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Manage customer shopping cart")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get cart items", description = "Retrieve the items in the current customer's or guest's cart")
    @ApiResponse(responseCode = "200", description = "Cart items retrieved successfully")
    @GetMapping("/items")
    public ResponseEntity<List<CartItemDTO>> getCartItems(
            @Parameter(description = "Session ID for guest users") @RequestParam(required = false) String sessionId) {
        String email = getCurrentUserEmail();
        log.info("GET /api/cart/items - email='{}', sessionId='{}'", email, sessionId);
        ValueWrapper<List<CartItemDTO>> items = cartService.getCartItemsForCurrentCustomer(email, sessionId);
        log.debug("Cart contains {} items", items.getValue().size());
        return ResponseEntity.ok(items.getValue());
    }

    @Operation(summary = "Add item to cart", description = "Add a new item to the cart of the logged-in user or guest")
    @ApiResponse(responseCode = "200", description = "Item added to cart successfully")
    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(
            @Parameter(description = "Session ID for guest users") @RequestParam(required = false) String sessionId,
            @RequestBody @Valid AddToCartRequest request) {
        String email = getCurrentUserEmail();
        log.info("POST /api/cart/add called with email='{}', sessionId='{}', productId={}, unitPrice={}, quantity={}",
                email, sessionId, request.getProductId(), request.getUnitPrice(), request.getQuantity());
        validateSessionOrEmail(email, sessionId);
        cartService.addToCart(email, sessionId, request.getProductId(), request.getUnitPrice(), request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update item quantity", description = "Update the quantity of a specific product in the cart")
    @ApiResponse(responseCode = "200", description = "Cart item quantity updated")
    @PutMapping("/update-quantity")
    public ResponseEntity<Void> updateQuantity(
            @Parameter(description = "Session ID for guest users") @RequestParam(required = false) String sessionId,
            @RequestBody @Valid UpdateCartRequest request) {
        String email = getCurrentUserEmail();
        log.info("PUT /api/cart/update-quantity called with email='{}', sessionId='{}', productId={}, quantity={}",
                email, sessionId, request.getProductId(), request.getQuantity());
        validateSessionOrEmail(email, sessionId);

        cartService.updateCartItemQuantity(email, sessionId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove item from cart", description = "Remove a specific product from the cart")
    @ApiResponse(responseCode = "200", description = "Item removed from cart")
    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeFromCart(
            @Parameter(description = "Session ID for guest users") @RequestParam(required = false) String sessionId,
            @Parameter(description = "ID of the product to remove from cart", required = true, example = "101") @RequestParam Long productId) {
        String email = getCurrentUserEmail();
        log.info("DELETE /api/cart/remove called with email='{}', sessionId='{}', productId={}", email, sessionId, productId);
        validateSessionOrEmail(email, sessionId);
        cartService.removeFromCart(email, sessionId, productId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Clear cart", description = "Remove all items from the cart")
    @ApiResponse(responseCode = "200", description = "Cart cleared successfully")
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(
            @Parameter(description = "Session ID for guest users") @RequestParam(required = false) String sessionId) {

        String email = getCurrentUserEmail();
        log.info("DELETE /api/cart/clear called with email='{}', sessionId='{}'", email, sessionId);
        validateSessionOrEmail(email, sessionId);
        cartService.clearCart(email, sessionId);
        return ResponseEntity.ok().build();
    }

}
