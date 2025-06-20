package org.store.app.service;

import org.store.app.common.ValueWrapper;
import org.store.app.dto.CustomerAddressDTO;
import org.store.app.enums.AddressType;
import org.store.app.model.CustomerAddress;

import java.util.List;

public interface CustomerAddressService {


    ValueWrapper<List<CustomerAddressDTO>> getAllAddressesForCurrentCustomer(Long customerId);

    CustomerAddressDTO createAddress(CustomerAddressDTO addressDTO, Long customerId);

    CustomerAddressDTO updateAddress(Long addressId, CustomerAddressDTO addressDTO, Long customerId);

    void softDeleteAddress(Long addressId, Long customerId);

    void verifyAddressOwnership(Long addressId, Long customerId);

    CustomerAddress getDefaultAddress(Long customerId, AddressType addressType) ;
}
