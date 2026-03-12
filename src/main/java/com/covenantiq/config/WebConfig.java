package com.covenantiq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig {

    private final boolean allowAllOrigins;
    private final List<String> allowedOriginPatterns;

    public WebConfig(
            @Value("${app.cors.allow-all-origins:true}") boolean allowAllOrigins,
            @Value("${app.cors.allowed-origin-patterns:http://localhost:5173,http://127.0.0.1:5173}") List<String> allowedOriginPatterns
    ) {
        this.allowAllOrigins = allowAllOrigins;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(
                allowAllOrigins ? List.of("*") : allowedOriginPatterns
        );
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.addAllowedHeader("*");
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
