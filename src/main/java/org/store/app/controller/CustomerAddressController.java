package org.store.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.store.app.common.ValueWrapper;
import org.store.app.dto.CustomerAddressDTO;
import org.store.app.security.userdetails.CustomUserDetails;
import org.store.app.service.CustomerAddressService;

import java.util.List;

@RestController
@RequestMapping("/store/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Customer Address API", description = "Manage customer addresses")
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    @Operation(summary = "Get all addresses for the current customer")
    @GetMapping
    public ResponseEntity<List<CustomerAddressDTO>> getAllAddressesForCurrentCustomer(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        ValueWrapper<List<CustomerAddressDTO>> address = addressService.getAllAddressesForCurrentCustomer(customerId);
        return ResponseEntity.ok(address.getValue());
    }

    @Operation(summary = "Create a new address for the current customer")
    @PostMapping
    public ResponseEntity<CustomerAddressDTO> createAddress(
            @Parameter(description = "New address data") @Valid @RequestBody CustomerAddressDTO addressDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        CustomerAddressDTO created = addressService.createAddress(addressDTO, customerId);
        return ResponseEntity.ok(created);
    }

    @Operation(summary = "Update an existing address for the current customer")
    @PutMapping("/{addressId}")
    public ResponseEntity<CustomerAddressDTO> updateAddress(
            @Parameter(description = "ID of the address to update") @PathVariable Long addressId,
            @Parameter(description = "Updated address data")@Valid @RequestBody CustomerAddressDTO addressDTO,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        addressDTO.setId(addressId);
        CustomerAddressDTO updated = addressService.updateAddress(addressId, addressDTO, customerId);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Soft delete an address for the current customer")
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @Parameter(description = "ID of the address to delete") @PathVariable Long addressId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long customerId = userDetails.getId();
        addressService.softDeleteAddress(addressId, customerId);
        return ResponseEntity.noContent().build();
    }
}
