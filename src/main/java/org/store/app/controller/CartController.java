package org.store.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.store.app.dto.AddToCartRequest;
import org.store.app.dto.CartDTO;
import org.store.app.dto.UpdateCartRequest;
import org.store.app.service.CartService;

import java.nio.file.AccessDeniedException;

import static org.store.app.util.RequestUtils.*;

@RestController
@RequestMapping("/store/api/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Manage customer shopping cart")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get cart", description = "Retrieve the cart for the current customer or guest")
    @ApiResponse(responseCode = "200", description = "Cart retrieved successfully")
    @Parameters({
            @Parameter(name = "sessionId", in = ParameterIn.COOKIE, description = "Session ID cookie for guest customers")
    })
    @GetMapping
    public ResponseEntity<CartDTO> getCart(HttpServletRequest servletRequest) {
        String email = getCurrentUserEmail();
        String sessionId = resolveSessionId(servletRequest);
        validateSessionOrEmail(email, sessionId);
        CartDTO cart = cartService.getActiveCart(email, sessionId);
        return ResponseEntity.ok(cart);
    }

    @Operation(summary = "Add item to cart", description = "Add a new item to the cart of the logged-in user or guest")
    @ApiResponse(responseCode = "200", description = "Item added to cart successfully")
    @Parameters({
            @Parameter(name = "sessionId", in = ParameterIn.COOKIE, description = "Session ID cookie for guest customers")
    })
    @PostMapping("/add")
    public ResponseEntity<Void> addToCart(HttpServletRequest servletRequest, @RequestBody @Valid AddToCartRequest request) throws AccessDeniedException {
        String email = getCurrentUserEmail();
        String sessionId = resolveSessionId(servletRequest);
        validateSessionOrEmail(email, sessionId);
        cartService.addToCart(email, sessionId, request.getProductId(), request.getUnitPrice(), request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update item quantity", description = "Update the quantity of a specific product in the cart")
    @ApiResponse(responseCode = "200", description = "Cart item quantity updated")
    @Parameters({
            @Parameter(name = "sessionId", in = ParameterIn.COOKIE, description = "Session ID cookie for guest customers")
    })
    @PutMapping("/update-quantity")
    public ResponseEntity<Void> updateQuantity(HttpServletRequest servletRequest, @RequestBody @Valid UpdateCartRequest request) {
        String sessionId = resolveSessionId(servletRequest);
        String email = getCurrentUserEmail();
        validateSessionOrEmail(email, sessionId);
        cartService.updateCartItemQuantity(email, sessionId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove item from cart", description = "Remove a specific product from the cart")
    @ApiResponse(responseCode = "200", description = "Item removed from cart")
    @Parameters({
            @Parameter(name = "sessionId", in = ParameterIn.COOKIE, description = "Session ID cookie for guest customers")
    })
    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeFromCart(
            HttpServletRequest servletRequest,
            @Parameter(description = "ID of the product to remove from cart", required = true, example = "101") @RequestParam Long productId) {
        String sessionId = resolveSessionId(servletRequest);
        String email = getCurrentUserEmail();
        validateSessionOrEmail(email, sessionId);
        cartService.removeFromCart(email, sessionId, productId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Clear cart", description = "Remove all items from the cart")
    @ApiResponse(responseCode = "200", description = "Cart cleared successfully")
    @Parameters({
            @Parameter(name = "sessionId", in = ParameterIn.COOKIE, description = "Session ID cookie for guest customers")
    })
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(@Parameter(hidden = true) HttpServletRequest servletRequest) {
        String sessionId = resolveSessionId(servletRequest);
        String email = getCurrentUserEmail();
        validateSessionOrEmail(email, sessionId);
        cartService.clearCart(email, sessionId);
        return ResponseEntity.ok().build();
    }

}
