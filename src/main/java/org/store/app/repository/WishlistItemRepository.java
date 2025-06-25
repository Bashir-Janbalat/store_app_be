package org.store.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.store.app.model.Wishlist;
import org.store.app.model.WishlistItem;
import org.store.app.projection.WishlistItemProductProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findByWishlist(Wishlist wishlist);
    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);

    Optional<WishlistItem> findByWishlistIdAndProductId(Long id, Long productId);

    void deleteAllByWishlistId(Long id);

    @Query(value = """
        SELECT 
            wi.product_id AS productId,
            p.selling_price AS unitPrice,
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
        FROM wishlist_items wi
        JOIN products p ON p.id = wi.product_id
        WHERE wi.wishlist_id = :wishlistId
        """, nativeQuery = true)
    List<WishlistItemProductProjection> findWishlistItemsWithProductInfo(@Param("wishlistId") Long wishlistId);
}
