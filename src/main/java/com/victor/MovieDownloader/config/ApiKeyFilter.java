package com.victor.MovieDownloader.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.victor.MovieDownloader.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * Requires a valid X-API-Key header on every /api/** request. The key is a shared secret between
 * this backend and trusted server-side callers (e.g. the Next.js app's route handlers) -- it is
 * never meant to reach a browser directly.
 */
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);
    private static final String HEADER_NAME = "X-API-Key";

    private final String expectedApiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKeyFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String providedKey = request.getHeader(HEADER_NAME);

        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            log.error("api-security.api-key is not configured; rejecting all API requests");
            writeUnauthorized(response, "API is not configured correctly");
            return;
        }

        if (providedKey == null || !constantTimeEquals(providedKey, expectedApiKey)) {
            writeUnauthorized(response, "Missing or invalid API key");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }
}
