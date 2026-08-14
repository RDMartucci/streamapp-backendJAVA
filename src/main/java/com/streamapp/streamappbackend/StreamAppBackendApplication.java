package com.streamapp.streamappbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.streamapp.streamappbackend",
    "com.streamapp.streamappbackend.config",
    "com.streamapp.streamappbackend.security",
    "com.streamapp.streamappbackend.entity",
    "com.streamapp.streamappbackend.repository",
    "com.streamapp.streamappbackend.service",
    "com.streamapp.streamappbackend.controller"
})
public class StreamAppBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamAppBackendApplication.class, args);
    }

}