package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.dto.AddPlaylistItemRequest;
import com.streamapp.streamappbackend.dto.CreatePlaylistRequest;
import com.streamapp.streamappbackend.dto.PlaylistDto;
import com.streamapp.streamappbackend.dto.PlaylistSummaryDto;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.repository.UserRepository;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.service.playlist.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Playlists del usuario autenticado. Cada playlist pertenece a su creador y
 * solo su owner puede verla/renombrarla/eliminarla y gestionar sus items.
 */
@RestController
@RequestMapping("/api/playlists")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final UserRepository userRepository;

    public PlaylistController(PlaylistService playlistService, UserRepository userRepository) {
        this.playlistService = playlistService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<PlaylistSummaryDto> list(Authentication authentication) {
        return playlistService.list(currentUser(authentication));
    }

    @GetMapping("/{id}")
    public PlaylistDto get(Authentication authentication, @PathVariable("id") Long id) {
        return playlistService.get(currentUser(authentication), id);
    }

    @PostMapping
    public ResponseEntity<PlaylistDto> create(Authentication authentication,
                                              @Valid @RequestBody CreatePlaylistRequest request) {
        PlaylistDto dto = playlistService.create(currentUser(authentication), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public PlaylistDto rename(Authentication authentication,
                              @PathVariable("id") Long id,
                              @Valid @RequestBody CreatePlaylistRequest request) {
        return playlistService.rename(currentUser(authentication), id, request.getName());
    }

    @DeleteMapping("/{id}")
    public void delete(Authentication authentication, @PathVariable("id") Long id) {
        playlistService.delete(currentUser(authentication), id);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<PlaylistDto> addItem(Authentication authentication,
                                               @PathVariable("id") Long id,
                                               @Valid @RequestBody AddPlaylistItemRequest request) {
        PlaylistDto dto = playlistService.addItem(currentUser(authentication), id, request.getPath());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{id}/items")
    public PlaylistDto removeItem(Authentication authentication,
                                  @PathVariable("id") Long id,
                                  @RequestParam("path") String path) {
        return playlistService.removeItem(currentUser(authentication), id, path);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}