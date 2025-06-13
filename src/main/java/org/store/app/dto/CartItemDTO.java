package org.store.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDTO {
    @Schema(description = "ID of the product", example = "123")
    private Long productId;
    @Schema(description = "Quantity of the product in the cart", example = "2")
    private int quantity;
    @Schema(description = "Unit price of the product", example = "19.99")
    private BigDecimal unitPrice;
    @Schema(description = "Basic product information")
    private ProductInfoDTO product;

}