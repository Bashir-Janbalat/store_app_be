package org.store.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.store.app.dto.*;
import org.store.app.security.jwt.JwtTokenProvider;
import org.store.app.security.userdetails.CustomUserDetails;
import org.store.app.service.*;

import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "Authentication", description = "Endpoints for customer authentication and password management")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final CustomerService customerService;
    private final JwtTokenProvider jwtTokenProvider;
    private final CartService cartService;
    private final WishlistService wishlistService;

    @Value("${app.cookie.secure}")
    private boolean secure;

    @Value("${app.cookie.same-site}")
    private String sameSite;

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

        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(secure) // اجعلها false فقط للتطوير على localhost بدون HTTPS
                .path("/")
                .maxAge(15 * 60) // مدة الصلاحية 15 دقيقة مثلا
                .sameSite(sameSite) // مهم لو كانت الواجهة والباك في دومينات مختلفة ويجب أن تكون None في Prod
                .build();

        if (sessionId != null) {
            log.info("Merging cart and wishlist for session ID: {} and user: {}", sessionId, loginDto.getEmail());
            cartService.mergeCartOnLogin(loginDto.getEmail(), sessionId);
            wishlistService.mergeWishlistOnLogin(loginDto.getEmail(), sessionId);
        }

        return ResponseEntity.ok()
                .header("Set-Cookie", cookie.toString())
                .build();
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
    public ResponseEntity<JwtPayload> getCurrentCustomer(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("Unauthorized access to /me endpoint");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("Retrieving profile for user: {}", userDetails.getEmail());
        JwtPayload payload = new JwtPayload(
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getName(),
                userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()
        );

        return ResponseEntity.ok(payload);
    }

    @Operation(summary = "Customer registration", description = "Register a new Customer account")
    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid CustomerDTO customerDTO) {
        authService.signup(customerDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Customer logout", description = "Invalidate the JWT token to log out customer")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = jwtTokenProvider.getTokenFromRequest(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            long expirationTimeMillis = jwtTokenProvider.getExpirationFromToken(token) - System.currentTimeMillis();
            log.info("Logging out token, adding to blacklist: {}", token);
            jwtTokenProvider.addTokenToBlacklist(token, expirationTimeMillis);
        } else {
            log.warn("No valid token found for logout");
        }

        ResponseCookie deleteCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(0)
                .sameSite(sameSite)
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", deleteCookie.toString())
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
}

