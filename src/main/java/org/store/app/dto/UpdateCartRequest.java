package org.store.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request DTO for updating the quantity of a product in the cart")
public class UpdateCartRequest {

    @NotNull(message = "Product ID is required")
    @Schema(description = "ID of the product to update", example = "101")
    private Long productId;
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "New quantity for the product in the cart", example = "2")
    private int quantity;
}
