package com.streamapp.streamappbackend.service.admin;

import com.streamapp.streamappbackend.dto.AdminDeleteResult;
import com.streamapp.streamappbackend.entity.MediaItem;
import com.streamapp.streamappbackend.entity.UserRoot;
import com.streamapp.streamappbackend.exception.ForbiddenException;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.repository.FavoriteRepository;
import com.streamapp.streamappbackend.repository.MediaItemRepository;
import com.streamapp.streamappbackend.repository.PlaylistRepository;
import com.streamapp.streamappbackend.repository.UserRootRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Operaciones de administración sobre el contenido servido. El admin puede
 * borrar archivos o carpetas (recursivo) dentro de las raíces configuradas por
 * CUALQUIER usuario, pero nunca una raíz en sí misma.
 *
 * Al borrar contenido se limpia también el catálogo (MediaItem) y las
 * referencias a favoritos y playlists para no dejar registros huérfanos.
 */
@Service
public class AdminMediaService {

    private final UserRootRepository userRootRepository;
    private final MediaItemRepository mediaItemRepository;
    private final FavoriteRepository favoriteRepository;
    private final PlaylistRepository playlistRepository;

    public AdminMediaService(UserRootRepository userRootRepository,
                             MediaItemRepository mediaItemRepository,
                             FavoriteRepository favoriteRepository,
                             PlaylistRepository playlistRepository) {
        this.userRootRepository = userRootRepository;
        this.mediaItemRepository = mediaItemRepository;
        this.favoriteRepository = favoriteRepository;
        this.playlistRepository = playlistRepository;
    }

    @Transactional
    public AdminDeleteResult deleteContent(String rawPath) {
        Path resolved = resolveAllowed(rawPath);

        if (!Files.exists(resolved)) {
            throw new NotFoundException("No existe: " + resolved);
        }

        long filesDeleted = deleteRecursively(resolved);

        int catalogItemsRemoved = cleanupCatalog(resolved);

        return new AdminDeleteResult(resolved.toString(), filesDeleted, catalogItemsRemoved);
    }

    /**
     * La ruta debe estar dentro de alguna raíz de cualquier usuario, pero no
     * puede ser la raíz misma (se protegen las ubicaciones configuradas).
     */
    private Path resolveAllowed(String rawPath) {
        if (rawPath.indexOf('\u0000') >= 0) {
            throw new ForbiddenException("Ruta inválida");
        }

        Path candidate = Paths.get(rawPath).toAbsolutePath().normalize();
        List<String> roots = userRootRepository.findAll().stream()
                .map(UserRoot::getPath)
                .toList();

        if (roots.isEmpty()) {
            throw new ForbiddenException("No hay raíces configuradas en el sistema");
        }

        boolean insideRoot = false;
        for (String root : roots) {
            Path rootPath = Paths.get(root).toAbsolutePath().normalize();
            if (candidate.startsWith(rootPath)) {
                if (candidate.equals(rootPath)) {
                    throw new ForbiddenException("No se puede borrar una raíz configurada: " + rootPath);
                }
                insideRoot = true;
                break;
            }
        }
        if (!insideRoot) {
            throw new ForbiddenException("La ruta está fuera de las raíces del sistema");
        }
        return candidate;
    }

    private long deleteRecursively(Path resolved) {
        if (!Files.isDirectory(resolved)) {
            try {
                Files.deleteIfExists(resolved);
                return 1;
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo borrar el archivo: " + resolved, e);
            }
        }

        try (Stream<Path> walk = Files.walk(resolved)) {
            List<Path> all = walk.sorted(Comparator.reverseOrder()).toList();
            long deleted = 0;
            for (Path p : all) {
                try {
                    if (Files.deleteIfExists(p)) {
                        deleted++;
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("No se pudo borrar: " + p, e);
                }
            }
            return deleted;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo recorrer el directorio: " + resolved, e);
        }
    }

    /**
     * Elimina del catálogo todos los MediaItem bajo la ruta borrada y sus
     * referencias en favoritos y playlists (los items huérfanos apuntarían a
     * archivos inexistentes).
     */
    private int cleanupCatalog(Path resolved) {
        List<MediaItem> affected = mediaItemRepository.findByPathPrefix(resolved.toString());
        if (affected.isEmpty()) {
            return 0;
        }

        List<Long> ids = affected.stream().map(MediaItem::getId).toList();

        favoriteRepository.deleteByMediaItemIn(affected);
        playlistRepository.deleteItemsByMediaItemIds(ids);
        mediaItemRepository.deleteAll(affected);

        return affected.size();
    }
}