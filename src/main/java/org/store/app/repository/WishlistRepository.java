package org.store.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.store.app.enums.WishlistStatus;
import org.store.app.model.Customer;
import org.store.app.model.Wishlist;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByCustomerAndStatus(Customer customer, WishlistStatus status);
    Optional<Wishlist> findBySessionIdAndStatus(String sessionId, WishlistStatus status);

    @Modifying
    @Query("DELETE FROM Wishlist c WHERE c.customer IS NULL AND c.createdAt < :cutoffDate")
    int deleteWishlistsWithoutCustomerBefore(LocalDateTime cutoffDate);
}
