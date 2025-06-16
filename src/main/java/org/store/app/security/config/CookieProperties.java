package org.store.app.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

    private String accessTokenName;
    private String refreshTokenName;
    private Long maxAgeRefreshToken;
    private boolean httpOnly;
    private boolean secure;
    private String sameSite;
}
