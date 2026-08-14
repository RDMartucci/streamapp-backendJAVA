package com.streamapp.streamappbackend.service.auth;

import com.streamapp.streamappbackend.dto.CreateUserRequest;
import com.streamapp.streamappbackend.entity.Role;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.exception.ConflictException;
import com.streamapp.streamappbackend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("El nombre de usuario ya está en uso: " + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(normalizeRole(request.getRole()));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    private Role normalizeRole(Role role) {
        // Los admins solo pueden crear ROLE_USER y ROLE_ADMIN (nunca ROLE_VISITOR).
        if (role == Role.ROLE_VISITOR) {
            return Role.ROLE_USER;
        }
        return role;
    }
}