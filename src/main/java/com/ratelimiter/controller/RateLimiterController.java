package com.ratelimiter.controller;

import com.ratelimiter.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    public RateLimiterController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/request")
    public ResponseEntity<String> handleRequest(HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        log.info("Incoming request from IP: {}", clientIp);

        if (rateLimiterService.isAllowed(clientIp)) {
            return ResponseEntity
                    .ok("Request accepted. Welcome!");
        } else {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body("Rate limit exceeded. Try again in 60 seconds.");
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String ip = request.getRemoteAddr();

        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }

        return ip;
    }
}