package org.store.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.app.enums.CartStatus;
import org.store.app.model.Cart;
import org.store.app.model.Customer;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomerAndStatus(Customer customer, CartStatus cartStatus);

    Optional<Cart> findBySessionIdAndStatus(String sessionId, CartStatus status);
    
}
