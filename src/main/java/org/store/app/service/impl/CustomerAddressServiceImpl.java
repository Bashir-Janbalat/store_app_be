package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.dto.CustomerAddressDTO;
import org.store.app.enums.AddressType;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.mapper.CustomerAddressMapper;
import org.store.app.model.Customer;
import org.store.app.model.CustomerAddress;
import org.store.app.repository.CustomerAddressRepository;
import org.store.app.repository.CustomerRepository;
import org.store.app.service.CustomerAddressService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final CustomerAddressMapper addressMapper;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "customerAddresses", key = "#customerId")
    public List<CustomerAddressDTO> getAllAddressesForCurrentCustomer(Long customerId) {
        log.info("Fetching all non-deleted addresses for customer id: {}", customerId);
        List<CustomerAddress> addresses = addressRepository.findByCustomerIdAndDeletedFalse(customerId);

        List<CustomerAddressDTO> result = addresses.stream().map(addressMapper::toDto).toList();
        log.info("Found {} addresses for customer id: {}", result.size(), customerId);
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = "customerAddresses", key = "#customerId")
    public CustomerAddressDTO createAddress(CustomerAddressDTO addressDTO, Long customerId) {
        log.info("Creating new address for customer id: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        CustomerAddress address = addressMapper.toEntity(addressDTO);
        address.setCustomer(customer);

        if (Boolean.TRUE.equals(address.getDefaultAddress())) {
            addressRepository.clearDefaultForCustomerAndType(customerId, address.getAddressType());
        }

        CustomerAddress saved = addressRepository.save(address);
        log.info("Created new address with id: {} for customer id: {}", saved.getId(), customerId);
        return addressMapper.toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "customerAddresses", key = "#customerId")
    public CustomerAddressDTO updateAddress(Long addressId, CustomerAddressDTO addressDTO, Long customerId) {
        log.info("Updating address with id: {} for customer id: {}", addressId, customerId);
        CustomerAddress existing = getAddressIfOwnedByCustomer(addressId, customerId);

        AddressType oldType = existing.getAddressType();
        AddressType newType = addressDTO.getAddressType() != null ? addressDTO.getAddressType() : oldType;

        // تحديث البيانات العادية
        existing.setAddressLine(addressDTO.getAddressLine());
        existing.setCity(addressDTO.getCity());
        existing.setState(addressDTO.getState());
        existing.setPostalCode(addressDTO.getPostalCode());
        existing.setCountry(addressDTO.getCountry());

        // إذا تغير النوع
        if (!oldType.equals(newType)) {
            log.info("Address type changed from {} to {} for address id {}", oldType, newType, addressId);

            if (Boolean.TRUE.equals(existing.getDefaultAddress())) {
                // إذا كان العنوان الحالي default في النوع القديم → احذفه من القديم
                addressRepository.clearDefaultForCustomerAndType(customerId, oldType);
                existing.setDefaultAddress(false);
                addressRepository.flush();
            }
            existing.setAddressType(newType);
        }

        // هل يريد المستخدم أن يجعل هذا العنوان default في النوع الجديد؟
        if (Boolean.TRUE.equals(addressDTO.getDefaultAddress())) {
            log.info("Setting address id {} as default for customer id {} and addressType {}", addressId, customerId, newType);

            addressRepository.clearDefaultForCustomerAndType(customerId, newType);
            addressRepository.flush();
            existing.setDefaultAddress(true);
        } else if (Boolean.FALSE.equals(addressDTO.getDefaultAddress())) {
            existing.setDefaultAddress(false);
            log.info("Address id {} default flag set to false", addressId);
        }

        CustomerAddress updated = addressRepository.save(existing);
        log.info("Address with id {} updated successfully", addressId);
        return addressMapper.toDto(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "customerAddresses", key = "#customerId")
    public void softDeleteAddress(Long addressId, Long customerId) {
        log.info("Soft deleting address id: {} for customer id: {}", addressId, customerId);
        CustomerAddress existing = getAddressIfOwnedByCustomer(addressId, customerId);
        existing.setDeleted(true);
        addressRepository.save(existing);
        log.info("Address id: {} marked as deleted", addressId);
    }

    @Override
    @Transactional
    public void verifyAddressOwnership(Long addressId, Long customerId) {
        log.debug("Verifying ownership for address id: {} and customer id: {}", addressId, customerId);
        getAddressIfOwnedByCustomer(addressId, customerId);
    }

    @Override
    public Long getDefaultAddressId(Long customerId, AddressType addressType) {
        return addressRepository.findDefaultAddressIdByCustomerIdAndType(customerId, addressType)
                .orElseThrow(() -> new ResourceNotFoundException("Default " + addressType.name().toLowerCase() + " address not found for customer with id: " + customerId));
    }

    private CustomerAddress getAddressIfOwnedByCustomer(Long addressId, Long customerId) {
        log.debug("Fetching address with id: {}", addressId);
        CustomerAddress address = addressRepository.findById(addressId).orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        if (address.getDeleted() != null && address.getDeleted()) {
            log.error("Address with id: {} is deleted", addressId);
            throw new ResourceNotFoundException("Address not found with id: " + addressId);
        }

        if (!address.getCustomer().getId().equals(customerId)) {
            log.warn("Access denied for customer id: {} to address id: {}", customerId, addressId);
            throw new AccessDeniedException("You are not authorized to access this address");
        }
        log.debug("Address with id: {} successfully verified for customer id: {}", addressId, customerId);
        return address;
    }

}
