package com.streamapp.streamappbackend.service.streaming;

import com.streamapp.streamappbackend.exception.UnauthorizedException;

public class StreamTicketException extends UnauthorizedException {

    public StreamTicketException(String message) {
        super(message);
    }
}