package com.neetrag.web;

import java.util.Map;

import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> unauthorized(UnauthorizedException e) {
        return error(HttpStatus.UNAUTHORIZED,
                "Claude API key missing or invalid. Set the ANTHROPIC_API_KEY environment variable and restart.");
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
