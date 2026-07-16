package com.specguard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Allows the dashboard frontend (a separate repo/origin, e.g. a Next.js app on
 * localhost:3000) to call this API from the browser. Without this, every request from
 * a different origin is blocked client-side even though the server would happily answer
 * it — CORS is enforced by the browser, not by this backend.
 *
 * Origin list comes from {@code specguard.frontend-origins} (see application.yml) so it
 * can differ between local dev and a real deployed frontend without a code change.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final FrontendProperties properties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.getOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}