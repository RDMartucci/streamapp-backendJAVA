package com.streamapp.streamappbackend.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String error,
        String message,
        Map<String, String> fieldErrors,
        Instant timestamp
) {
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, null, Instant.now());
    }

    public static ErrorResponse of(String error, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(error, message, fieldErrors, Instant.now());
    }
}