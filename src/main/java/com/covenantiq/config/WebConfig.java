package com.covenantiq.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final boolean allowAllOrigins;
    private final List<String> allowedOriginPatterns;

    public WebConfig(
            @Value("${app.cors.allow-all-origins:false}") boolean allowAllOrigins,
            @Value("${app.cors.allowed-origin-patterns:http://localhost:5173,http://127.0.0.1:5173}") List<String> allowedOriginPatterns
    ) {
        this.allowAllOrigins = allowAllOrigins;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowAllOrigins ? new String[]{"*"} : allowedOriginPatterns.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
