package org.store.app.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddToCartRequest {
    private String sessionId;
    private Long productId;
    private BigDecimal unitPrice;
    private int quantity;
}