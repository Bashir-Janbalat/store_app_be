package org.store.app.dto;

import lombok.Data;

@Data
public class UpdateCartRequest {
    private String sessionId;
    private Long productId;
    private int quantity;
}
