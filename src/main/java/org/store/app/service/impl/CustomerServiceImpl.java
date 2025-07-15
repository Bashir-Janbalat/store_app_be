package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.store.app.dto.CustomerDTO;
import org.store.app.exception.AlreadyExistsException;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.mapper.CustomerMapper;
import org.store.app.model.Customer;
import org.store.app.model.Role;
import org.store.app.repository.CustomerRepository;
import org.store.app.repository.RoleRepository;
import org.store.app.service.CustomerService;

import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        if (customerRepository.existsByEmail(customerDTO.getEmail())) {
            log.warn("Attempt to create customer with an existing email: {}", customerDTO.getEmail());
            throw new AlreadyExistsException("user", "email", customerDTO.getEmail());
        }

        Customer customer = customerMapper.toEntity(customerDTO);
        customer.setPassword(passwordEncoder.encode(customerDTO.getPassword()));

        Optional<Role> role = roleRepository.findRoleByName("ROLE_CUSTOMER");
        if (role.isPresent()) {
            customer.setRoles(Set.of(role.get()));
            log.info("Assigned 'ROLE_CUSTOMER' to customer: {}", customerDTO.getName());
        } else {
            log.error("Role 'ROLE_CUSTOMER' not found, customer {} cannot be created", customerDTO.getName());
            throw new IllegalStateException("Role 'ROLE_CUSTOMER' not found");
        }

        Customer saved = customerRepository.save(customer);
        log.info("Customer with name '{}' and email '{}' successfully created", customer.getName(), customer.getEmail());
        return customerMapper.toDto(saved);
    }

    @Override
    public void updatePassword(String email, String newPassword) {
        log.info("Attempting to update password for customer with email '{}'", email);
        Customer customer = customerRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Customer with email " + email + " not found"));
        customer.setPassword(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);
        log.info("Password updated successfully for customer '{}'", email);
    }

    @Override
    public void updateNameAndPhone(Long customerId, String name, String phone, String dialCode, String countryCode) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setName(name);
        customer.setPhone(phone);
        customer.setDialCode(dialCode);
        customer.setCountryCode(countryCode);

        customerRepository.save(customer);
    }

    @Override
    public void markEmailVerified(String email) {
        Customer customer = customerRepository.findByEmail(email).orElseThrow(() ->
                new ResourceNotFoundException("Customer not found"));
        customer.setEmailVerified(true);
        customerRepository.save(customer);
    }
}
