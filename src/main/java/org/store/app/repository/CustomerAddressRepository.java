package org.store.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.store.app.enums.AddressType;
import org.store.app.model.CustomerAddress;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerIdAndDeletedFalse(Long customerId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CustomerAddress c SET c.defaultAddress = false " +
           "WHERE c.customer.id = :customerId AND c.addressType = :type AND c.deleted = false")
    void clearDefaultForCustomerAndType(@Param("customerId") Long customerId, @Param("type") AddressType type);


    @Query(" SELECT a.id FROM CustomerAddress a " +
           "WHERE a.customer.id = :customerId " +
           "AND a.addressType = :addressType " +
           "AND a.defaultAddress = true " +
           "AND a.deleted = false ")
    Optional<Long> findDefaultAddressIdByCustomerIdAndType(@Param("customerId") Long customerId, @Param("addressType") AddressType addressType);
}