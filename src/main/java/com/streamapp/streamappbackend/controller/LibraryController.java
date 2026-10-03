package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.dto.LibraryScanResult;
import com.streamapp.streamappbackend.dto.LibraryPageDto;
import com.streamapp.streamappbackend.dto.MediaItemDto;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.repository.UserRepository;
import com.streamapp.streamappbackend.service.library.LibraryCatalogService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryCatalogService libraryCatalogService;
    private final UserRepository userRepository;

    public LibraryController(LibraryCatalogService libraryCatalogService,
                              UserRepository userRepository) {
        this.libraryCatalogService = libraryCatalogService;
        this.userRepository = userRepository;
    }

    @PostMapping("/scan")
    public LibraryScanResult scan(Authentication authentication) {
        return libraryCatalogService.scan(currentUser(authentication));
    }

    @GetMapping("/items")
    public List<MediaItemDto> items(Authentication authentication) {
        return libraryCatalogService.listDtos(currentUser(authentication));
    }

    @GetMapping
    public LibraryPageDto page(Authentication authentication,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "24") int size,
                               @RequestParam(required = false) String q,
                               @RequestParam(defaultValue = "all") String type) {
        return libraryCatalogService.page(currentUser(authentication), page, size, q, type);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}
