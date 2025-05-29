package org.store.app.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.dto.CustomerDTO;
import org.store.app.dto.LoginDTO;
import org.store.app.security.jwt.JwtTokenProvider;
import org.store.app.service.AuthService;
import org.store.app.service.CustomerService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomerService customerService;

    @Override
    public String login(LoginDTO loginDto) {
        log.info("Customer '{}' attempting to log in", loginDto.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getEmail(),
                        loginDto.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);

        log.info("Customer '{}' successfully authenticated", loginDto.getEmail());

        return token;
    }

    @Override
    @Transactional
    public void signup(CustomerDTO customerDTO) {
        log.info("Customer '{}' attempting to sign up", customerDTO.getEmail());
        customerService.createCustomer(customerDTO);
        log.info("Customer '{}' successfully signed up", customerDTO.getEmail());
    }
}

