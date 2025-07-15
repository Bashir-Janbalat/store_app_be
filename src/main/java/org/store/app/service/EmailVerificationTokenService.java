package org.store.app.service;

import org.store.app.dto.EmailVerificationTokenDTO;
import org.store.app.model.EmailVerificationToken;

import java.util.Optional;

public interface EmailVerificationTokenService {

    void createTokenFor(String email);

    Optional<EmailVerificationTokenDTO> validateToken(String token);

    void markTokenAsUsedByToken(String token);

    void resendVerificationLink(String email);
}
