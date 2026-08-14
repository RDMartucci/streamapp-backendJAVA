package com.streamapp.streamappbackend.service.explorer;

import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.exception.ForbiddenException;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Valida que una ruta solicitada esté dentro de las raíces configuradas por el
 * usuario. Es la única puerta de acceso al filesystem para el streaming, el
 * explorador, favoritos y playlists (previene path traversal).
 */
@Component
public class PathAccessValidator {

    private final UserRootsService userRootsService;

    public PathAccessValidator(UserRootsService userRootsService) {
        this.userRootsService = userRootsService;
    }

    /** Normaliza y devuelve la ruta absoluta si está dentro de alguna raíz del usuario. */
    public Path resolveWithinRoots(User user, String rawPath) {
        if (rawPath.indexOf('\u0000') >= 0) {
            throw new ForbiddenException("Ruta inválida");
        }

        Path candidate = Paths.get(rawPath).toAbsolutePath().normalize();

        for (String root : userRootsService.getRoots(user)) {
            Path rootPath = Paths.get(root).toAbsolutePath().normalize();
            if (candidate.startsWith(rootPath)) {
                return candidate;
            }
        }
        throw new ForbiddenException("La ruta está fuera de las ubicaciones permitidas para este usuario");
    }
}