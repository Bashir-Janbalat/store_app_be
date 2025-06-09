package org.store.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to add a product to the wishlist")
public class AddToWishlistRequest {

    @NotNull(message = "productId is required")
    @Schema(description = "ID of the product to add", example = "101")
    private Long productId;
}
