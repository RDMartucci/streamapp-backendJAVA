package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.dto.UserDto;
import com.streamapp.streamappbackend.entity.Role;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.repository.UserRepository;
import com.streamapp.streamappbackend.exception.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado: " + username));
        return toDto(user);
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(),
                user.getRole() != null ? user.getRole() : Role.ROLE_VISITOR,
                user.getCreatedAt() != null ? user.getCreatedAt() : Instant.EPOCH);
    }
}