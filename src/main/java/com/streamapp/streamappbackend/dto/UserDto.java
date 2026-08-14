package com.streamapp.streamappbackend.dto;

import com.streamapp.streamappbackend.entity.Role;

import java.time.Instant;

public record UserDto(Long id, String username, Role role, Instant createdAt) {
}