package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.dto.AdminDeleteResult;
import com.streamapp.streamappbackend.service.admin.AdminMediaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operaciones de administración sobre el contenido. Solo ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin/media")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMediaController {

    private final AdminMediaService adminMediaService;

    public AdminMediaController(AdminMediaService adminMediaService) {
        this.adminMediaService = adminMediaService;
    }

    /**
     * Borra un archivo o carpeta (recursivo) dentro de las raíces del sistema.
     * La ruta se identifica por su path absoluto (igual que explorer/streaming).
     */
    @DeleteMapping
    public AdminDeleteResult delete(@RequestParam("path") String path) {
        return adminMediaService.deleteContent(path);
    }
}