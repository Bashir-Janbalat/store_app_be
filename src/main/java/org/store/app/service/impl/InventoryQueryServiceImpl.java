package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.store.app.service.InventoryQueryService;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryQueryServiceImpl implements InventoryQueryService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int getAvailableStock(Long productId) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock WHERE product_id = ?";
        Integer quantity = jdbcTemplate.queryForObject(sql, Integer.class, productId);
        return quantity != null ? quantity : 0;
    }
}
