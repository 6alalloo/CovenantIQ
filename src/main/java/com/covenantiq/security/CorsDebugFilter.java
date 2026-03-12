package com.covenantiq.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CorsDebugFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorsDebugFilter.class);

    private final boolean requestDebugEnabled;

    public CorsDebugFilter(@Value("${app.request-debug.enabled:false}") boolean requestDebugEnabled) {
        this.requestDebugEnabled = requestDebugEnabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!requestDebugEnabled) {
            return true;
        }

        if (!request.getRequestURI().startsWith("/api/")) {
            return true;
        }

        return request.getHeader("Origin") == null
                && request.getHeader("Access-Control-Request-Method") == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Map<String, String> requestHeaders = new LinkedHashMap<>();
        requestHeaders.put("origin", request.getHeader("Origin"));
        requestHeaders.put("host", request.getHeader("Host"));
        requestHeaders.put("forwarded", request.getHeader("Forwarded"));
        requestHeaders.put("xForwardedProto", request.getHeader("X-Forwarded-Proto"));
        requestHeaders.put("xForwardedHost", request.getHeader("X-Forwarded-Host"));
        requestHeaders.put("xForwardedPort", request.getHeader("X-Forwarded-Port"));
        requestHeaders.put("accessControlRequestMethod", request.getHeader("Access-Control-Request-Method"));
        requestHeaders.put("accessControlRequestHeaders", request.getHeader("Access-Control-Request-Headers"));

        log.info("cors-debug request method={} uri={} headers={}", request.getMethod(), request.getRequestURI(), requestHeaders);

        filterChain.doFilter(request, response);

        Map<String, String> responseHeaders = new LinkedHashMap<>();
        responseHeaders.put("accessControlAllowOrigin", response.getHeader("Access-Control-Allow-Origin"));
        responseHeaders.put("accessControlAllowMethods", response.getHeader("Access-Control-Allow-Methods"));
        responseHeaders.put("accessControlAllowHeaders", response.getHeader("Access-Control-Allow-Headers"));
        responseHeaders.put("vary", response.getHeader("Vary"));

        log.info(
                "cors-debug response method={} uri={} status={} headers={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                responseHeaders
        );
    }
}
