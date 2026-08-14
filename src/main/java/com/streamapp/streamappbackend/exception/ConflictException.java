package com.streamapp.streamappbackend.exception;

public class ConflictException extends AppException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}