package org.store.app.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class RequestUtils {

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
}
