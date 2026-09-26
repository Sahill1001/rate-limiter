package com.ratelimiter;

import com.ratelimiter.controller.RateLimiterController;
import com.ratelimiter.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimiterApplicationTests {
    private final RateLimiterService service = mock(RateLimiterService.class);
    private final RateLimiterController controller = new RateLimiterController(service);

    @Test void acceptsAnAllowedRequest() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        when(service.isAllowed("192.0.2.10")).thenReturn(true);
        var response = controller.handleRequest(request);
        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getHeaders().getFirst("Retry-After"));
    }

    @Test void rejectsAnExhaustedClientWithRetryHint() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        var response = controller.handleRequest(request);
        assertEquals(429, response.getStatusCode().value());
        assertEquals("60", response.getHeaders().getFirst("Retry-After"));
    }

    @Test void usesTheFirstForwardedAddress() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "192.0.2.10, 192.0.2.20");
        controller.handleRequest(request);
        verify(service).isAllowed("192.0.2.10");
    }

    @Test void normalizesIpv6Loopback() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("::1");
        controller.handleRequest(request);
        verify(service).isAllowed("127.0.0.1");
    }
}
