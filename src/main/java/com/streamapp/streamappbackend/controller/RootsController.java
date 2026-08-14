package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.repository.UserRepository;
import com.streamapp.streamappbackend.dto.RootRequest;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.service.explorer.UserRootsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Gestiona las rutas de streaming de CADA usuario autenticado.
 * Cualquier usuario logueado puede administrar sus propias rutas (máx 5).
 */
@RestController
@RequestMapping("/api/roots")
public class RootsController {

    private final UserRootsService userRootsService;
    private final UserRepository userRepository;

    public RootsController(UserRootsService userRootsService, UserRepository userRepository) {
        this.userRootsService = userRootsService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<String> listRoots(Authentication authentication) {
        return userRootsService.getRoots(currentUser(authentication));
    }

    @PostMapping
    public ResponseEntity<List<String>> addRoot(Authentication authentication,
                                                @Valid @RequestBody RootRequest request) {
        List<String> roots = userRootsService.addRoot(currentUser(authentication), request.getPath());
        return ResponseEntity.status(HttpStatus.CREATED).body(roots);
    }

    @DeleteMapping
    public List<String> removeRoot(Authentication authentication,
                                   @RequestParam("path") String path) {
        return userRootsService.removeRoot(currentUser(authentication), path);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}