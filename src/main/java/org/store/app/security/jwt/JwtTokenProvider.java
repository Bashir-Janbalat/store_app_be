package org.store.app.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.store.app.security.config.CookieProperties;
import org.store.app.security.userdetails.CustomUserDetails;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret.key}")
    private String jwtSecret;
    @Value("${jwt.expiration.time}")
    private long jwtExpirationTime;

    @Value("${jwt.refresh.secret.key}")
    private String jwtRefreshSecret;

    @Value("${jwt.refresh.expiration.time}")
    private long jwtRefreshExpirationTime;


    @Value("${jwt.reset.expiration.time}")
    private long jwtResetExpirationTime;

    @Value("${jwt.reset.secret.key}")
    private String jwtResetSecret;

    @Value("${jwt.email.verification.secret}")
    private String jwtEmailVerificationSecret;

    @Value("${jwt.email.verification.expiration.time}")
    private long jwtEmailVerificationExpirationTime;

    private final RedisTemplate<String, String> redisTemplate;

    private final UserDetailsService userDetailsService;

    private final CookieProperties cookieProperties;


    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String username = userDetails.getUsername();
        String name = userDetails.getName();
        Long id = userDetails.getId();

        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationTime);

        return Jwts.builder()
                .subject(username)
                .claim("id", id)
                .claim("name", name)
                .claim("roles", userDetails.getAuthorities())
                .issuedAt(currentDate)
                .expiration(expireDate)
                .signWith(key())
                .compact();
    }

    public String generateToken(String username) {

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        return generateToken(authentication);
    }

    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public long getExpirationFromToken(String token) {
        return Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(token).getPayload().getExpiration().getTime();

    }

    public boolean validateToken(String token) {
        Jwts.parser().verifyWith((SecretKey) key()).build().parse(token);
        return true;
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieProperties.getAccessTokenName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void addTokenToBlacklist(String token, long expirationMillis) {
        redisTemplate.opsForValue().set(token, "blacklisted", expirationMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey(token);
    }

    public String generatePasswordResetToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtResetExpirationTime);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtResetSecret)))
                .compact();
    }

    public boolean validatePasswordResetToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtResetSecret)))
                    .build()
                    .parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
// ---------  Refresh Token ---------

    public String generateRefreshToken(String username) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtRefreshExpirationTime);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getRefreshKey())
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtRefreshSecret)))
                    .build().parse(token);
            return !isTokenBlacklisted(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromRefreshToken(String token) {
        return Jwts.parser().verifyWith((SecretKey) getRefreshKey()).build().parseSignedClaims(token).getPayload().getSubject();
    }

    private Key getRefreshKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtRefreshSecret));
    }

    public long getExpirationFromRefreshToken(String token) {
        return Jwts.parser().verifyWith((SecretKey) getRefreshKey()).build().parseSignedClaims(token).getPayload().getExpiration().getTime();

    }

    // ---------  Email Verification Token ---------

    public String generateEmailVerificationToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtEmailVerificationExpirationTime);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtEmailVerificationSecret)))
                .compact();
    }


    public boolean validateEmailVerificationToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtEmailVerificationSecret)))
                    .build()
                    .parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public String getEmailFromEmailVerificationToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtEmailVerificationSecret)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
}
