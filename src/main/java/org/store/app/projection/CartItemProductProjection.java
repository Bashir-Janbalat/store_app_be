package org.store.app.projection;

import java.math.BigDecimal;

public interface CartItemProductProjection {
    
    Long getProductId();

    int getQuantity();

    BigDecimal getUnitPrice();

    String getName();

    String getDescription();

    String getImageUrl();
}
