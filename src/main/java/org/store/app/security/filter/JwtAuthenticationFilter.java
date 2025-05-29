package org.store.app.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.store.app.exception.ErrorResponse;
import org.store.app.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = jwtTokenProvider.getTokenFromRequest(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                if (jwtTokenProvider.isTokenBlacklisted(token)) {
                    logger.warn("Token blacklisted: {}", token);
                    SecurityContextHolder.clearContext();
                    sendErrorResponse(response, "Token blacklisted. Please login again.", HttpStatus.UNAUTHORIZED, request.getRequestURI());
                    return;
                }
                String username = jwtTokenProvider.getUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            logger.warn("Expired JWT: {}", ex.getMessage());
            sendErrorResponse(response, "Token expired. Please login again.", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (JwtException ex) {
            logger.warn("Invalid JWT: {}", ex.getMessage());
            sendErrorResponse(response, "JWT token is invalid.", HttpStatus.UNAUTHORIZED, request.getRequestURI());
        } catch (Exception ex) {
            logger.error("Unexpected JWT validation error: {}", ex.getMessage(), ex);
            sendErrorResponse(response, "Authentication service unavailable. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR, request.getRequestURI());
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message, HttpStatus status, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        errorResponse.setStatus(status.value());
        errorResponse.setError(status.getReasonPhrase());
        errorResponse.setMessage(message);
        errorResponse.setPath(path);

        objectMapper.writeValue(response.getWriter(), errorResponse);
        response.flushBuffer();
    }
}


