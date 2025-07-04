package org.store.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.store.app.model.ProductReview;

import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    boolean existsByProductIdAndCustomerId(Long productId, Long customerId);

    List<ProductReview> findAllByProductId(Long productId);

    @Query(value = "SELECT id FROM products WHERE id = :productId", nativeQuery = true)
    Long findProductId(@Param("productId") Long productId);
}
