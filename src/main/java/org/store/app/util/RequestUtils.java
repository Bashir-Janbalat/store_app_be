package org.store.app.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class RequestUtils {

    private static final String SESSION_COOKIE_NAME = "sessionId";

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return null;
    }

    public static void validateSessionOrEmail(String email, String sessionId) {
        if ((email == null || email.isBlank()) && (sessionId == null || sessionId.isBlank())) {
            throw new IllegalArgumentException("Must provide either email or sessionId");
        }
    }

    public static String resolveSessionId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            return java.util.Arrays.stream(request.getCookies())
                    .filter(cookie -> SESSION_COOKIE_NAME.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
