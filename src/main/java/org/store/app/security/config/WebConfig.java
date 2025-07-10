package org.store.app.security.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors_allowed_origin}")
    private String corsAllowedOrigin;

    @PostConstruct
    public void init() {
        log.info("Allowed Origins = {}", corsAllowedOrigin);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = corsAllowedOrigin.split(",");
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("X-XSRF-TOKEN", "Authorization", "Content-Type","Cache-Control")
                .exposedHeaders("X-XSRF-TOKEN")
                .allowCredentials(true);
    }
}
