package com.covenantiq.config;

import com.covenantiq.security.CorrelationIdFilter;
import com.covenantiq.security.CorsDebugFilter;
import com.covenantiq.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CorrelationIdFilter correlationIdFilter,
            CorsDebugFilter corsDebugFilter,
            ObjectMapper objectMapper,
            CorsConfigurationSource corsConfigurationSource,
            @Value("${app.security.enabled:true}") boolean securityEnabled
    ) throws Exception {
        if (!securityEnabled) {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.addHeaderWriter(
                        new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)
                ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/app/**",
                                "/login",
                                "/index.html",
                                "/assets/**",
                                "/api/v1/auth/**",
                                "/api/v1/runtime-config",
                                "/actuator/health",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            Map<String, Object> body = new LinkedHashMap<>();
                            body.put("title", "Unauthorized");
                            body.put("status", HttpStatus.UNAUTHORIZED.value());
                            body.put("detail", "Authentication is required");
                            body.put("instance", request.getRequestURI());
                            body.put("timestamp", OffsetDateTime.now().toString());
                            objectMapper.writeValue(response.getOutputStream(), body);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                            Map<String, Object> body = new LinkedHashMap<>();
                            body.put("title", "Forbidden");
                            body.put("status", HttpStatus.FORBIDDEN.value());
                            body.put("detail", "You do not have permission to perform this action");
                            body.put("instance", request.getRequestURI());
                            body.put("timestamp", OffsetDateTime.now().toString());
                            objectMapper.writeValue(response.getOutputStream(), body);
                        })
                )
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(corsDebugFilter, CorrelationIdFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, CorrelationIdFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
