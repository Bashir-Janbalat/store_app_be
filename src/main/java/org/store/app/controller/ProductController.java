package org.store.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.store.app.common.ValueWrapper;
import org.store.app.dto.AddReviewRequest;
import org.store.app.dto.ProductReviewDTO;
import org.store.app.security.userdetails.CustomUserDetails;
import org.store.app.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Operations related to product listing and reviews")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Add a review for a product", description = "Allows an authenticated customer to add a review for a product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review added successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – customer not authenticated"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping("/{productId}/reviews")
    public ResponseEntity<?> addReview(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AddReviewRequest request,
            @Parameter(description = "ID of the product", example = "1") @PathVariable Long productId) {

        Long customerId = userDetails.getId();
        productService.AddReview(customerId, productId, request.getRating(), request.getReview());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get all reviews for a product", description = "Returns a list of all reviews for a given product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved reviews"),
    })
    @GetMapping("/{productId}/reviews")
    public ResponseEntity<List<ProductReviewDTO>> getReviewsFor(
            @Parameter(description = "ID of the product", example = "1") @PathVariable Long productId) {

        ValueWrapper<List<ProductReviewDTO>> result = productService.getReviewsFor(productId);
        return ResponseEntity.ok().body(result.getValue());
    }

}
