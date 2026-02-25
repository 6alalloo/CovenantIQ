package com.covenantiq.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void reusesIncomingCorrelationIdAndPopulatesMdcDuringRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/loans");
        request.addHeader("X-Correlation-ID", "cid-test-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> correlationIdInChain = new AtomicReference<>();
        AtomicReference<String> requestPathInChain = new AtomicReference<>();

        FilterChain chain = (req, res) -> {
            correlationIdInChain.set(MDC.get("correlationId"));
            requestPathInChain.set(MDC.get("requestPath"));
        };

        filter.doFilter(request, response, chain);

        assertEquals("cid-test-123", correlationIdInChain.get());
        assertEquals("/api/v1/loans", requestPathInChain.get());
        assertEquals("cid-test-123", response.getHeader("X-Correlation-ID"));
        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestPath"));
    }

    @Test
    void generatesCorrelationIdWhenMissingAndClearsMdcAfterCompletion() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            assertEquals("/actuator/health", MDC.get("requestPath"));
            assertNotNull(MDC.get("correlationId"));
            assertFalse(MDC.get("correlationId").isBlank());
        };

        filter.doFilter(request, response, chain);

        String responseCorrelationId = response.getHeader("X-Correlation-ID");
        assertNotNull(responseCorrelationId);
        assertTrue(responseCorrelationId.length() >= 16);
        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("requestPath"));
        assertNull(MDC.get("responseStatus"));
    }
}
