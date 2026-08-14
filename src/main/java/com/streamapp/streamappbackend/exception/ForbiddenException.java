package com.streamapp.streamappbackend.exception;

public class ForbiddenException extends AppException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}