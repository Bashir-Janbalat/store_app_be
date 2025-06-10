package org.store.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.app.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

}
