package org.store.app.service;

import org.store.app.dto.CustomerAddressDTO;
import org.store.app.enums.AddressType;

import java.util.List;

public interface CustomerAddressService {


    List<CustomerAddressDTO> getAllAddressesForCurrentCustomer(Long customerId);

    CustomerAddressDTO createAddress(CustomerAddressDTO addressDTO, Long customerId);

    CustomerAddressDTO updateAddress(Long addressId, CustomerAddressDTO addressDTO, Long customerId);

    void softDeleteAddress(Long addressId, Long customerId);

    void verifyAddressOwnership(Long addressId, Long customerId);

    Long getDefaultAddressId(Long customerId, AddressType addressType) ;
}
