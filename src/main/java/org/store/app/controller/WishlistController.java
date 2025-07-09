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
import org.store.app.common.ValueWrapper;
import org.store.app.dto.AddToWishlistRequest;
import org.store.app.dto.WishlistItemDTO;
import org.store.app.service.WishlistService;

import java.nio.file.AccessDeniedException;
import java.util.List;

import static org.store.app.util.RequestUtils.*;

@RestController
@RequestMapping("/store/api/wishlist")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wishlist", description = "Manage customer wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    @Operation(summary = "Get wishlist items", description = "Retrieve the items in the current customer's or guest's wishlist")
    @ApiResponse(responseCode = "200", description = "Wishlist items retrieved successfully")
    @Parameter(name = "sessionId", in = ParameterIn.COOKIE, description = "Session ID cookie for guest customers")
    @GetMapping("/items")
    public ResponseEntity<List<WishlistItemDTO>> getWishlistItems(HttpServletRequest request) {
        String email = getCurrentUserEmail();
        String sessionId = resolveSessionId(request);
        validateSessionOrEmail(email, sessionId);
        ValueWrapper<List<WishlistItemDTO>> items = wishlistService.getWishlistItemsForCurrentCustomer(email, sessionId);
        return ResponseEntity.ok(items.getValue());
    }

    @Operation(summary = "Add item to wishlist", description = "Add a new item to the wishlist of the logged-in user or guest")
    @ApiResponse(responseCode = "200", description = "Item added to wishlist successfully")
    @Parameter(name = "sessionId", in = ParameterIn.COOKIE, description = "Session ID cookie for guest customers")
    @PostMapping("/add")
    public ResponseEntity<Void> addToWishlist(HttpServletRequest request, @Valid @RequestBody AddToWishlistRequest dto) throws AccessDeniedException {
        String email = getCurrentUserEmail();
        String sessionId = resolveSessionId(request);
        validateSessionOrEmail(email, sessionId);
        wishlistService.addToWishlist(email, sessionId, dto.getProductId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove item from wishlist", description = "Remove a specific product from the wishlist")
    @ApiResponse(responseCode = "200", description = "Item removed from wishlist")
    @Parameters({
            @Parameter(name = "sessionId", in = ParameterIn.COOKIE, description = "Session ID cookie for guest customers"),
            @Parameter(name = "productId", description = "ID of the product to remove from wishlist", required = true, example = "101")
    })
    @DeleteMapping("/remove")
    public ResponseEntity<Void> removeFromWishlist(
            HttpServletRequest request,
            @RequestParam Long productId) {
        String email = getCurrentUserEmail();
        String sessionId = resolveSessionId(request);
        validateSessionOrEmail(email, sessionId);
        wishlistService.removeFromWishlist(email, sessionId, productId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Clear wishlist", description = "Remove all items from the wishlist")
    @ApiResponse(responseCode = "200", description = "Wishlist cleared successfully")
    @Parameter(name = "sessionId", in = ParameterIn.COOKIE, description = "Session ID cookie for guest customers")
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearWishlist(HttpServletRequest request) {
        String email = getCurrentUserEmail();
        String sessionId = resolveSessionId(request);
        validateSessionOrEmail(email, sessionId);
        wishlistService.clearWishlist(email, sessionId);
        return ResponseEntity.ok().build();
    }
}
