package com.streamapp.streamappbackend.service.explorer;

import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.entity.UserRoot;
import com.streamapp.streamappbackend.exception.ConflictException;
import com.streamapp.streamappbackend.repository.UserRootRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

@Service
public class UserRootsService {

    public static final int MAX_ROOTS = 5;

    private final UserRootRepository userRootRepository;

    public UserRootsService(UserRootRepository userRootRepository) {
        this.userRootRepository = userRootRepository;
    }

    @Transactional(readOnly = true)
    public List<String> getRoots(User user) {
        return userRootRepository.findAllByUser(user).stream()
                .map(UserRoot::getPath)
                .toList();
    }

    @Transactional
    public List<String> addRoot(User user, String root) {
        String normalized = normalize(root);
        if (userRootRepository.countByUser(user) >= MAX_ROOTS) {
            throw new ConflictException("Alcanzaste el máximo de " + MAX_ROOTS
                    + " rutas por usuario. Eliminá una ruta antes de agregar otra.");
        }
        if (userRootRepository.findByUserAndPath(user, normalized).isPresent()) {
            throw new ConflictException("La ruta ya está configurada: " + normalized);
        }

        UserRoot ur = new UserRoot();
        ur.setUser(user);
        ur.setPath(normalized);
        userRootRepository.save(ur);

        return getRoots(user);
    }

    @Transactional
    public List<String> removeRoot(User user, String root) {
        String normalized = normalize(root);
        userRootRepository.findByUserAndPath(user, normalized).ifPresent(userRootRepository::delete);
        return getRoots(user);
    }

    private String normalize(String root) {
        String trimmed = root == null ? "" : root.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("La ruta no puede estar vacía");
        }
        try {
            return Path.of(trimmed).toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ruta no válida: " + trimmed);
        }
    }
}