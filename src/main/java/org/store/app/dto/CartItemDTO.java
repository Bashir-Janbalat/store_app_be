package org.store.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.store.app.projection.CartItemProductProjection;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDTO {
    private Long productId;
    private int quantity;
    private BigDecimal unitPrice;
    private ProductInfoDTO product;

    public CartItemDTO(CartItemProductProjection projection) {
        this.productId = projection.getProductId();
        this.quantity = projection.getQuantity();
        this.unitPrice = projection.getUnitPrice();
        this.product = new ProductInfoDTO(
                projection.getName(),
                projection.getDescription(),
                projection.getImageUrl()
        );
    }
}