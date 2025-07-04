package org.store.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.store.app.model.Cart;
import org.store.app.model.CartItem;
import org.store.app.projection.CartItemProductProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {


    @Query(value = """
            SELECT 
                ci.product_id AS productId,
                ci.quantity AS quantity,
                ci.unit_price AS unitPrice,
                p.name AS name,
                p.description AS description,
                (
                    SELECT i.image_url 
                    FROM images i 
                    WHERE i.product_id = p.id 
                    ORDER BY i.id ASC 
                    LIMIT 1
                ) AS imageUrl,
                (
                SELECT COALESCE(SUM(s.quantity), 0)
                FROM stock s
                WHERE s.product_id = p.id
                ) AS totalStock
            FROM cart_items ci
            JOIN products p ON p.id = ci.product_id
            WHERE ci.cart_id = :cartId
            """, nativeQuery = true)
    List<CartItemProductProjection> findCartItemsWithProductInfo(@Param("cartId") Long cartId);

    Optional<CartItem> findByCartIdAndProductId(Long id, Long productId);


    List<CartItem> findByCart(Cart sessionCart);


    @Query("SELECT ci.quantity FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.productId = :productId")
    Optional<Integer> findQuantityByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);
}

