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
import org.store.app.dto.AddToWishlistRequest;
import org.store.app.dto.WishlistItemDTO;
import org.store.app.service.WishlistService;

import java.util.List;

import static org.store.app.util.RequestUtils.getCurrentUserEmail;
import static org.store.app.util.RequestUtils.validateSessionOrEmail;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wishlist", description = "Manage customer wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(summary = "Get wishlist items", description = "Retrieve the items in the current customer's or guest's wishlist")
    @ApiResponse(responseCode = "200", description = "Wishlist items retrieved successfully")
    @GetMapping("/items")
    public ResponseEntity<List<WishlistItemDTO>> getWishlistItems(
            @Parameter(description = "Session ID for guest users") @RequestParam(required = false) String sessionId) {
        String email = getCurrentUserEmail();
        log.info("GET /api/wishlist/items - email='{}', sessionId='{}'", email, sessionId);
        ValueWrapper<List<WishlistItemDTO>> items = wishlistService.getWishlistItemsForCurrentCustomer(email, sessionId);
        log.debug("Wishlist contains {} items", items.getValue().size());
        return ResponseEntity.ok(items.getValue());
    }

    @Operation(summary = "Add item to wishlist", description = "Add a new item to the wishlist of the logged-in user or guest")
    @ApiResponse(responseCode = "200", description = "Item added to wishlist successfully")
    @PostMapping("/add")
    public ResponseEntity<Void> addToWishlist(
            @Parameter(description = "Session ID for guest users") @RequestParam(required = false) String sessionId,
            @Valid @RequestBody AddToWishlistRequest request) {
        String email = getCurrentUserEmail();
        log.info("POST /api/wishlist/add - email='{}', sessionId='{}', productId={}", email, sessionId, request.getProductId());
        validateSessionOrEmail(email, sessionId);
        wishlistService.addToWishlist(email, sessionId, request.getProductId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove item from wishlist", description = "Remove a specific product from the wishlist")
    @ApiResponse(responseCode = "200", description = "Item removed from wishlist")
    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeFromWishlist(
            @Parameter(description = "Session ID for guest users") @RequestParam(required = false) String sessionId,
            @Parameter(description = "ID of the product to remove from wishlist", required = true, example = "101") @RequestParam Long productId) {
        String email = getCurrentUserEmail();
        log.info("DELETE /api/wishlist/remove - email='{}', sessionId='{}', productId={}", email, sessionId, productId);
        validateSessionOrEmail(email, sessionId);
        wishlistService.removeFromWishlist(email, sessionId, productId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Clear wishlist", description = "Remove all items from the wishlist")
    @ApiResponse(responseCode = "200", description = "Wishlist cleared successfully")
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearWishlist(
            @Parameter(description = "Session ID for guest users") @RequestParam(required = false) String sessionId) {
        String email = getCurrentUserEmail();
        log.info("DELETE /api/wishlist/clear - email='{}', sessionId='{}'", email, sessionId);
        validateSessionOrEmail(email, sessionId);
        wishlistService.clearWishlist(email, sessionId);
        return ResponseEntity.ok().build();
    }

}
