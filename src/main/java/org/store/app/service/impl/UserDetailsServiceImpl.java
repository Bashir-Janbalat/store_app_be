package org.store.app.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.model.Customer;
import org.store.app.repository.CustomerRepository;
import org.store.app.security.userdetails.CustomUserDetails;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Customer not found by email: {}", email);
                    return new UsernameNotFoundException("User not found: " + email);
                });

        Set<SimpleGrantedAuthority> authorities = customer.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        return new CustomUserDetails(
                customer.getId(),
                customer.getEmail(),
                customer.getName(),
                customer.getPassword(),
                customer.getPhone(),
                customer.getCountryCode(),
                customer.getDialCode(),
                authorities
        );
    }
}
