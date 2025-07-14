package org.store.app.service.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.app.dto.PasswordResetTokenDTO;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.model.Customer;
import org.store.app.model.PasswordResetToken;
import org.store.app.repository.CustomerRepository;
import org.store.app.repository.PasswordResetTokenRepository;
import org.store.app.security.jwt.JwtTokenProvider;
import org.store.app.service.EmailService;
import org.store.app.service.PasswordResetTokenService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Data
@Slf4j
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final CustomerRepository customerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Value("${domain}")
    private String domain;

    @Override
    @Transactional
    public PasswordResetToken createTokenFor(String email) {
        Customer customer = customerRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + email));
        String jwtToken = jwtTokenProvider.generatePasswordResetToken(customer.getEmail());
        String resetLink = domain + "/reset-password" + "?token=" + jwtToken;
        PasswordResetToken token = PasswordResetToken.builder()
                .token(jwtToken)
                .customer(customer)
                .used(false)
                .build();
        emailService.sendHtmlMail(
                customer.getEmail(),
                "Reset Your Password",
                "<p>Hello " + customer.getName() + ",</p>" +
                "<p>Click the following link to reset your password:</p>" +
                "<p><a href=\"" + resetLink + "\">Reset Password</a></p>" +
                "<p>If you didn't request this, please ignore this email.</p>"
        );
        log.info("Generated password reset link for customer {}", customer.getEmail());
        return passwordResetTokenRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetTokenDTO> validateToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
                .filter(t -> jwtTokenProvider.validatePasswordResetToken(t.getToken()) && !t.isUsed())
                .map(t -> new PasswordResetTokenDTO(t.getCustomer().getEmail()));

    }

    @Override
    @Transactional
    public void markTokenAsUsedByToken(String token) {
        passwordResetTokenRepository.findByToken(token).ifPresent(t -> {
            t.setUsed(true);
            passwordResetTokenRepository.save(t);
        });
    }
}
