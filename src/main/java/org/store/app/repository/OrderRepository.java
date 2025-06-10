package org.store.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.app.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}