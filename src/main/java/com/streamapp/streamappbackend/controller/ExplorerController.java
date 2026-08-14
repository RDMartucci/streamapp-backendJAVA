package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.dto.ExplorerNode;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.repository.UserRepository;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.service.explorer.FileExplorerService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/explorer")
public class ExplorerController {

    private final FileExplorerService fileExplorerService;
    private final UserRepository userRepository;

    public ExplorerController(FileExplorerService fileExplorerService, UserRepository userRepository) {
        this.fileExplorerService = fileExplorerService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<ExplorerNode> list(Authentication authentication,
                                   @RequestParam(value = "path", required = false) String path) {
        User user = currentUser(authentication);
        return fileExplorerService.list(user, path);
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }
}