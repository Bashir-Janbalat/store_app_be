package org.store.app.projection;

public interface ProductInfoProjection {
    Long getProductId();
    String getName();
    String getDescription();
    String getImageUrl();
}
