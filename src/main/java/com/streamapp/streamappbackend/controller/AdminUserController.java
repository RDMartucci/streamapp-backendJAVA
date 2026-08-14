package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.dto.CreateUserRequest;
import com.streamapp.streamappbackend.dto.UserDto;
import com.streamapp.streamappbackend.entity.Role;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.service.auth.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = adminUserService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(user));
    }

    @GetMapping
    public List<UserDto> listUsers() {
        return adminUserService.listUsers().stream().map(this::toDto).collect(Collectors.toList());
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(),
                user.getRole() != null ? user.getRole() : Role.ROLE_VISITOR,
                user.getCreatedAt());
    }
}