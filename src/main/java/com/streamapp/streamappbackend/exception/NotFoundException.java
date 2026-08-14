package com.streamapp.streamappbackend.exception;

public class NotFoundException extends AppException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}