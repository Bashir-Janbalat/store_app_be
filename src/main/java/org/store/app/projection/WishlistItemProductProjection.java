package org.store.app.projection;

import java.math.BigDecimal;

public interface WishlistItemProductProjection {
    Long getProductId();
    BigDecimal getUnitPrice();
    String getName();
    String getDescription();
    String getImageUrl();
    Long getTotalStock();
}
