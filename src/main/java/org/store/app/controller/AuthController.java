package org.store.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.store.app.dto.*;
import org.store.app.security.jwt.JwtTokenProvider;
import org.store.app.service.AuthService;
import org.store.app.service.CustomerService;
import org.store.app.service.PasswordResetTokenService;

import java.util.Optional;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for user authentication and password management")
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetTokenService passwordResetTokenService;
    private final CustomerService customerService;

    @Operation(summary = "Customer login", description = "Authenticate customer and generate JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginDTO loginDto) {

        String token = authService.login(loginDto);

        AuthResponseDTO authResponseDto = new AuthResponseDTO();
        authResponseDto.setAccessToken(token);

        return new ResponseEntity<>(authResponseDto, HttpStatus.OK);
    }

    @Operation(summary = "Customer registration", description = "Register a new Customer account")
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid CustomerDTO customerDTO) {
        authService.signup(customerDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Customer logout", description = "Invalidate the JWT token to log out customer")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        long expiration = jwtTokenProvider.getExpirationFromToken(token) - System.currentTimeMillis();
        jwtTokenProvider.addTokenToBlacklist(token, expiration);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Request password reset link", description = "Send password reset link to customer's email")
    @PostMapping("/send-reset-link")
    public ResponseEntity<String> requestReset(@RequestParam @Email String email) {
        passwordResetTokenService.createTokenFor(email);
        return ResponseEntity.ok("Password reset link sent to your email.");
    }


    @Operation(summary = "Reset password", description = "Reset customer's password using reset token")
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid PasswordResetRequestDTO request) {
        Optional<PasswordResetTokenDTO> resetTokenDTO = passwordResetTokenService.validateToken(request.getToken());
        if (resetTokenDTO.isPresent()) {
            customerService.updatePassword(resetTokenDTO.get().getEmail(), request.getNewPassword());
            passwordResetTokenService.markTokenAsUsedByToken(request.getToken());
            return ResponseEntity.ok("Password successfully reset.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token.");
    }
}

