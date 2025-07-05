package org.store.app.service;

public interface InventoryQueryService {
    int getAvailableStock(Long productId);
}
