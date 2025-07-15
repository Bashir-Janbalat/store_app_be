package org.store.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.store.app.dto.*;
import org.store.app.security.config.CookieProperties;
import org.store.app.security.jwt.JwtTokenProvider;
import org.store.app.security.userdetails.CustomUserDetails;
import org.store.app.service.*;
import org.store.app.service.impl.UserDetailsServiceImpl;

import java.net.URI;
import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/store/api/auth")
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for customer authentication and password management")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final CustomerService customerService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final CookieProperties cookieProperties;
    private final UserDetailsServiceImpl userDetailsService;
    private final EmailVerificationTokenService emailVerificationTokenService;

    @Value("${jwt.expiration.time}")
    private long jwtExpirationTime;

    @Value("${domain}")
    private String domain;

    private Long getMaxAgeAccessToken() {
        return jwtExpirationTime / 1000;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Customer login",
            description = "Authenticate customer and return JWT token as HttpOnly cookie." +
                          " Optionally merge cart and wishlist from guest session.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login successful, token set in cookie"),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials")
            }
    )
    public ResponseEntity<Void> login(
            @Parameter(description = "Session ID for guest users to merge their cart and wishlist on login")
            @RequestParam(required = false) String sessionId,
            @RequestBody LoginDTO loginDto) {
        String token = authService.login(loginDto);

        ResponseCookie accessTokenCookie = createCookie(cookieProperties.getAccessTokenName(), token, getMaxAgeAccessToken());
        String username = jwtTokenProvider.getUsername(token);
        String refreshToken = authService.generateRefreshToken(username);
        ResponseCookie refreshTokenCookie = createCookie(cookieProperties.getRefreshTokenName(), refreshToken, cookieProperties.getMaxAgeRefreshToken());


        if (sessionId != null) {
            cartService.mergeCartOnLogin(loginDto.getEmail(), sessionId);
            wishlistService.mergeWishlistOnLogin(loginDto.getEmail(), sessionId);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Set-Cookie", accessTokenCookie.toString());
        headers.add("Set-Cookie", refreshTokenCookie.toString());
        return ResponseEntity.ok().headers(headers).build();
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token is missing or invalid");
        }

        String username = jwtTokenProvider.getUsernameFromRefreshToken(refreshToken);

        String newAccessToken = authService.generateToken(username);

        ResponseCookie accessTokenCookie = createCookie(cookieProperties.getAccessTokenName(), newAccessToken, getMaxAgeAccessToken());

        return ResponseEntity.ok()
                .header("Set-Cookie", accessTokenCookie.toString())
                .body("Access token refreshed successfully");
    }

    @Operation(
            summary = "Get current authenticated customer",
            description = "Returns the details of the currently authenticated customer based on the JWT token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved current customer info"),
            @ApiResponse(responseCode = "401", description = "Unauthorized – JWT token is missing or invalid")
    })
    @GetMapping("/me")
    public ResponseEntity<JwtPayload> getCurrentCustomer(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("Unauthorized access to /me endpoint");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Retrieving profile for user: {}", userDetails.getEmail());
        JwtPayload payload = new JwtPayload(
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getName(),
                userDetails.getPhone(),
                userDetails.getCountryCode(),
                userDetails.getDialCode(),
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
        );

        return ResponseEntity.ok(payload);
    }

    @PutMapping("/me")
    @Operation(summary = "Update customer profile", description = "Update name and phone of the authenticated customer")
    public ResponseEntity<JwtPayload> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UpdateProfileDTO updateProfileDTO) {

        if (userDetails == null) {
            log.warn("Unauthorized access to update profile");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("User [{}] is updating profile.", userDetails.getEmail());
        customerService.updateNameAndPhone(
                userDetails.getId(),
                updateProfileDTO.getName(),
                updateProfileDTO.getPhone(),
                updateProfileDTO.getDialCode(),
                updateProfileDTO.getCountryCode()
        );
        CustomUserDetails updatedUser = (CustomUserDetails) userDetailsService.loadUserByUsername(userDetails.getEmail());

        JwtPayload updatedPayload = new JwtPayload(
                updatedUser.getId(),
                updatedUser.getEmail(),
                updatedUser.getName(),
                updatedUser.getPhone(),
                updatedUser.getCountryCode(),
                updatedUser.getDialCode(),
                updatedUser.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
        );
        log.info("User [{}] profile updated successfully", userDetails.getEmail());
        return ResponseEntity.ok(updatedPayload);
    }

    @Operation(summary = "Customer registration", description = "Register a new Customer account")
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid CustomerDTO customerDTO) {
        authService.signup(customerDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Customer logout", description = "Invalidate the JWT token to log out customer")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "access_token", required = false) String accessToken,
                                       @CookieValue(name = "refresh_token", required = false) String refreshToken) {
        blacklistTokenIfValid(accessToken, false);
        blacklistTokenIfValid(refreshToken, true);

        ResponseCookie deleteAccessTokenCookie = createCookie(cookieProperties.getAccessTokenName(), "", 0);
        ResponseCookie deleteRefreshTokenCookie = createCookie(cookieProperties.getRefreshTokenName(), "", 0);

        return ResponseEntity.ok()
                .header("Set-Cookie", deleteAccessTokenCookie.toString())
                .header("Set-Cookie", deleteRefreshTokenCookie.toString())
                .build();
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

    @Operation(summary = "Verify customer email", description = "Activate customer account via email verification link")
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        Optional<EmailVerificationTokenDTO> opt = emailVerificationTokenService.validateToken(token);
        if (opt.isPresent()) {
            customerService.markEmailVerified(opt.get().getEmail());
            emailVerificationTokenService.markTokenAsUsedByToken(token);
            URI redirectUri = URI.create(domain + "/verified?status=success");
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(redirectUri);
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
        URI redirectUri = URI.create(domain + "/verified?status=failed");
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirectUri);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @Operation(
            summary = "Resend email verification link",
            description = "Send a new email verification link to the customer's email if the account is not yet verified."
    )
    @PostMapping("/resend-verification-link")
    public ResponseEntity<?> resendVerificationLink(@RequestParam String email) {
        emailVerificationTokenService.resendVerificationLink(email);
        return ResponseEntity.ok("Verification link resent.");
    }

    private ResponseCookie createCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(cookieProperties.isHttpOnly())
                .secure(cookieProperties.isSecure())
                .path("/")
                .maxAge(maxAge)
                .sameSite(cookieProperties.getSameSite())
                .build();
    }

    private void blacklistTokenIfValid(String token, boolean isRefreshToken) {
        if (token != null) {
            boolean valid = isRefreshToken ? jwtTokenProvider.validateRefreshToken(token) : jwtTokenProvider.validateToken(token);
            if (valid) {
                long expirationTimeMillis = isRefreshToken
                        ? jwtTokenProvider.getExpirationFromRefreshToken(token) - System.currentTimeMillis()
                        : jwtTokenProvider.getExpirationFromToken(token) - System.currentTimeMillis();
                log.info("Logging out {} token adding to blacklist: {}", isRefreshToken ? "refresh" : "access", token);
                jwtTokenProvider.addTokenToBlacklist(token, expirationTimeMillis);
            } else {
                log.warn("Invalid {} token received during logout", isRefreshToken ? "refresh" : "access");
            }
        } else {
            log.warn("No {} token provided during logout", isRefreshToken ? "refresh" : "access");
        }
    }

}

