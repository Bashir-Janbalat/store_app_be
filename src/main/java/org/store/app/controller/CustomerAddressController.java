package org.store.app.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.store.app.dto.CustomerAddressDTO;
import org.store.app.security.userdetails.CustomUserDetails;
import org.store.app.service.CustomerAddressService;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    @GetMapping
    public ResponseEntity<List<CustomerAddressDTO>> getAllAddressesForCurrentCustomer(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        List<CustomerAddressDTO> address = addressService.getAllAddressesForCurrentCustomer(customerId);
        return ResponseEntity.ok(address);
    }


    @PostMapping
    public ResponseEntity<CustomerAddressDTO> createAddress(@Valid @RequestBody CustomerAddressDTO addressDTO,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        CustomerAddressDTO created = addressService.createAddress(addressDTO, customerId);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<CustomerAddressDTO> updateAddress(@PathVariable Long addressId, @RequestBody CustomerAddressDTO addressDTO,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        addressDTO.setId(addressId);
        CustomerAddressDTO updated = addressService.updateAddress(addressId, addressDTO, customerId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        addressService.softDeleteAddress(addressId, customerId);
        return ResponseEntity.noContent().build();
    }
}
