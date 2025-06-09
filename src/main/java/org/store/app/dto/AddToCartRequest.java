package org.store.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request body for adding a product to the cart")
public class AddToCartRequest {

    @NotNull(message = "Product ID is required")
    @Schema(description = "ID of the product to be added to the cart", example = "123")
    private Long productId;

    @NotNull(message = "Unit price is required")
    @Positive(message = "Unit price must be positive")
    @Schema(description = "Price per unit of the product", example = "49.99")
    private BigDecimal unitPrice;
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of the product to add", example = "2", minimum = "1", defaultValue = "1")
    private int quantity;
}