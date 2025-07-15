package org.store.app.service.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.store.app.dto.EmailVerificationTokenDTO;
import org.store.app.exception.ResourceNotFoundException;
import org.store.app.model.Customer;
import org.store.app.model.EmailVerificationToken;
import org.store.app.repository.CustomerRepository;
import org.store.app.repository.EmailVerificationTokenRepository;
import org.store.app.security.jwt.JwtTokenProvider;
import org.store.app.service.EmailService;
import org.store.app.service.EmailVerificationTokenService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Data
@Slf4j
public class EmailVerificationTokenServiceImpl implements EmailVerificationTokenService {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final CustomerRepository customerRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Value("${domain}")
    private String domain;

    @Override
    public void createTokenFor(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + email));

        String token = jwtTokenProvider.generateEmailVerificationToken(email);

        EmailVerificationToken entity = new EmailVerificationToken();
        entity.setToken(token);
        entity.setUsed(false);
        entity.setCustomer(customer);

        emailVerificationTokenRepository.save(entity);

        String verifyLink = domain + "/store/api/auth/verify-email?token=" + token;

        String htmlContent =
                "<p>Hello,</p>" +
                "<p>Thank you for registering at our store!</p>" +
                "<p>Please click the link below to verify your email address and activate your account:</p>" +
                "<p><a href=\"" + verifyLink + "\">Verify Email</a></p>" +
                "<br/>" +
                "<p>If you did not register for an account, please ignore this email.</p>" +
                "<p>Best regards,<br/>The OurStore Team</p>";

        emailService.sendHtmlMail(
                email,
                "Verify Your Email Address",
                htmlContent
        );

        log.info("Verification email sent to {} with url: {}", email, verifyLink);

    }

    @Override
    public Optional<EmailVerificationTokenDTO> validateToken(String token) {

        Optional<EmailVerificationToken> optToken = emailVerificationTokenRepository.findByToken(token);
        if (optToken.isEmpty() || optToken.get().isUsed()) {
            log.warn("Invalid or already used verification token: {}", token);
            return Optional.empty();
        }

        if (!jwtTokenProvider.validateEmailVerificationToken(token)) {
            log.warn("JWT verification token is invalid or expired: {}", token);
            return Optional.empty();
        }

        String email = jwtTokenProvider.getEmailFromEmailVerificationToken(token);
        EmailVerificationTokenDTO dto = new EmailVerificationTokenDTO(token, email, false);
        return Optional.of(dto);
    }

    @Override
    public void markTokenAsUsedByToken(String token) {
        Optional<EmailVerificationToken> opt = emailVerificationTokenRepository.findByToken(token);
        if (opt.isPresent() && !opt.get().isUsed()) {
            EmailVerificationToken entity = opt.get();
            entity.setUsed(true);
            emailVerificationTokenRepository.save(entity);
            log.info("Marked verification token as used: {}", token);
        }
    }

    @Override
    public void resendVerificationLink(String email) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + email));
        if (!customer.isEmailVerified()) {
            createTokenFor(email);
            return;
        }
        throw new IllegalStateException("Customer already verified or not registered.");
    }
}
