package com.neetrag.web;

import java.util.Map;

import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorHandler.class);

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> unauthorized(UnauthorizedException e) {
        log.warn("Claude rejected the API key: {}", e.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Claude API key missing or invalid. Put it in secrets.yml "
                + "(see secrets.example.yml) or the ANTHROPIC_API_KEY environment variable, then restart.");
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, String>> rateLimited(RateLimitException e) {
        return error(HttpStatus.TOO_MANY_REQUESTS, "Claude API rate limit reached. Try again shortly.");
    }

    @ExceptionHandler(AnthropicServiceException.class)
    public ResponseEntity<Map<String, String>> claudeError(AnthropicServiceException e) {
        return error(HttpStatus.BAD_GATEWAY, "Claude API error " + e.statusCode() + ": " + e.getMessage());
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
