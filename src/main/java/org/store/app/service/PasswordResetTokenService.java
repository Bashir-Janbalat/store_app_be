package org.store.app.service;

import org.store.app.dto.PasswordResetTokenDTO;
import org.store.app.model.PasswordResetToken;

import java.util.Optional;

public interface PasswordResetTokenService {

    PasswordResetToken createTokenFor(String email);

    Optional<PasswordResetTokenDTO> validateToken(String token);

    void markTokenAsUsedByToken(String token);
}
