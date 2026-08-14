package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.dto.AddFavoriteRequest;
import com.streamapp.streamappbackend.dto.FavoriteDto;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.repository.UserRepository;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.service.favorite.FavoriteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Favoritos del usuario autenticado. Solo roles USER y ADMIN (requieren
 * explorador/roots para poder marcar elementos reales).
 */
@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    public FavoriteController(FavoriteService favoriteService, UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<FavoriteDto> list(Authentication authentication) {
        return favoriteService.list(currentUser(authentication));
    }

    @PostMapping
    public ResponseEntity<FavoriteDto> add(Authentication authentication,
                                           @Valid @RequestBody AddFavoriteRequest request) {
        FavoriteDto dto = favoriteService.add(currentUser(authentication), request.getPath());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping
    public void remove(Authentication authentication,
                       @RequestParam("path") String path) {
        favoriteService.remove(currentUser(authentication), path);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}