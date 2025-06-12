package org.store.app.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.stereotype.Component;
import org.store.app.security.filter.JwtAuthenticationFilter;

@Component
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {


    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAuthenticationFilter authenticationFilter;
    private final Environment environment;

    @Value("${security.disabled:false}")
    private boolean securityDisabled;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (isDevProfile() && securityDisabled) {
            log.warn("⚠️⚠️ Running in DEV mode: Security is disabled, all endpoints are open! ⚠️⚠️");
            http.csrf(AbstractHttpConfigurer::disable);
            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        } else {
            http.cors(Customizer.withDefaults())
                    .csrf(csrf -> csrf
                            .ignoringRequestMatchers("/api/auth/**", "/api/cart/**", "/api/wishlist/**")
                            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    ).authorizeHttpRequests((authorize) -> {
                        authorize.requestMatchers("/api/auth/**").permitAll();
                        authorize.requestMatchers("/api/cart/**").permitAll();
                        authorize.requestMatchers("/api/wishlist/**").permitAll();
                        authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                        authorize.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                        authorize.anyRequest().authenticated();
                    });

            http.addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint));

        return http.build();
    }

    private boolean isDevProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.equalsIgnoreCase("dev")) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
