package com.streamapp.streamappbackend.dto;

import com.streamapp.streamappbackend.entity.Role;

public record AuthResponse(String token, String username, Role role, long expiresIn) {
}