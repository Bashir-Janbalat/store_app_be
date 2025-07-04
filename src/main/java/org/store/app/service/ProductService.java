package org.store.app.service;

import org.store.app.common.ValueWrapper;
import org.store.app.dto.ProductReviewDTO;

import java.util.List;

public interface ProductService {

    void AddReview(Long customerId, Long productId, Double rating, String review);

    ValueWrapper<List<ProductReviewDTO>> getReviewsFor(Long productId);
}
