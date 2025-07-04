package org.store.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.store.app.enums.OrderStatus;
import org.store.app.model.Order;
import org.store.app.projection.ProductInfoProjection;

import java.util.List;
import java.util.Set;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);

    @Query(value = """
            SELECT 
                p.id as productId,
                p.name,
                p.description,
                (
                    SELECT i.image_url 
                    FROM images i 
                    WHERE i.product_id = p.id
                    LIMIT 1
                ) AS image_url
            FROM products p
            WHERE p.id IN (:productIds)
            """, nativeQuery = true)
    List<ProductInfoProjection> findProductInfosByIds(@Param("productIds") Set<Long> productIds);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
           "WHERE oi.order.customer.id = :customerId " +
           "AND oi.productId = :productId " +
           "AND oi.order.status = :status")
    boolean hasCustomerPurchasedProduct(@Param("customerId") Long customerId,
                                        @Param("productId") Long productId,
                                        @Param("status") OrderStatus status);

}