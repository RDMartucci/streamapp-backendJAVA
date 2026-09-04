package com.streamapp.streamappbackend.service.explorer;

import com.streamapp.streamappbackend.dto.ExplorerNode;
import com.streamapp.streamappbackend.entity.MediaItem.MediaType;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.repository.MediaItemRepository;
import com.streamapp.streamappbackend.service.streaming.StreamTicketService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class FileExplorerService {

    private final UserRootsService userRootsService;
    private final MediaTypeResolver mediaTypeResolver;
    private final StreamTicketService streamTicketService;
    private final PathAccessValidator pathAccessValidator;
    private final MediaItemRepository mediaItemRepository;

    public FileExplorerService(UserRootsService userRootsService,
                               MediaTypeResolver mediaTypeResolver,
                               StreamTicketService streamTicketService,
                               PathAccessValidator pathAccessValidator,
                               MediaItemRepository mediaItemRepository) {
        this.userRootsService = userRootsService;
        this.mediaTypeResolver = mediaTypeResolver;
        this.streamTicketService = streamTicketService;
        this.pathAccessValidator = pathAccessValidator;
        this.mediaItemRepository = mediaItemRepository;
    }

    /**
     * Lista el contenido de una ruta para un usuario. Si path es null/vacío
     * devuelve las raíces configuradas por ESE usuario.
     */
    public List<ExplorerNode> list(User user, String path) {
        if (path == null || path.isBlank()) {
            return listRoots(user);
        }
        return listDirectory(user, path);
    }

    private List<ExplorerNode> listRoots(User user) {
        List<String> roots = userRootsService.getRoots(user);
        if (roots.isEmpty()) {
            return List.of();
        }

        List<ExplorerNode> nodes = new ArrayList<>();
        for (String root : roots) {
            Path p = Paths.get(root);
            if (!Files.exists(p)) {
                continue;
            }
            nodes.add(nodeForDirectory(root.toString(),
                    p.getFileName() != null ? p.getFileName().toString() : root));
        }
        nodes.sort(Comparator.comparing(n -> n.name().toLowerCase(Locale.ROOT)));
        return nodes;
    }

    private List<ExplorerNode> listDirectory(User user, String rawPath) {
        Path resolved = resolveAllowed(user, rawPath);
        if (!Files.isDirectory(resolved)) {
            throw new NotFoundException("No existe el directorio: " + rawPath);
        }

        List<ExplorerNode> nodes = new ArrayList<>();
        try (var stream = Files.list(resolved)) {
            stream.forEach(p -> {
                String name = p.getFileName().toString();
                if (Files.isDirectory(p)) {
                    nodes.add(node(user, p, name));
                } else if (Files.isRegularFile(p) && isVisibleMediaFile(name)) {
                    nodes.add(node(user, p, name));
                }
            });
        } catch (IOException e) {
            throw new NotFoundException("No se pudo leer el directorio: " + rawPath);
        }

        nodes.sort(Comparator
                .comparing(ExplorerNode::directory)
                .thenComparing(n -> n.name().toLowerCase(Locale.ROOT)));
        return nodes;
    }

    private boolean isVisibleMediaFile(String name) {
        return mediaTypeResolver.resolveType(name) != null;
    }

    private ExplorerNode nodeForDirectory(String path, String name) {
        return new ExplorerNode(name, path, true, null, false, null, null, null, null, null, null, null, null);
    }

    private ExplorerNode node(User user, Path p, String name) {
        boolean dir = Files.isDirectory(p);
        MediaType type = dir ? null : mediaTypeResolver.resolveType(name);
        boolean playable = !dir && type != null;
        long size = 0;
        Instant modified = null;
        try {
            if (!dir) {
                size = Files.size(p);
            }
            modified = Files.getLastModifiedTime(p).toInstant();
        } catch (IOException ignored) {
            // atributos no críticos
        }
        String streamUrl = playable
                ? "/api/stream/" + streamTicketService.generate(user.getUsername(), p.toString())
                : null;
        String posterUrl = null;
        Integer year = null;
        String genres = null;
        Double vote = null;
        String mediaTypeDetail = null;
        if (playable) {
            var opt = mediaItemRepository.findByNormalizedPath(p.toString().replace('\\', '/'));
            if (opt.isPresent()) {
                var m = opt.get();
                posterUrl = m.getPosterUrl();
                year = m.getYear();
                genres = m.getGenres();
                vote = m.getVoteAverage();
                mediaTypeDetail = m.getMediaTypeDetail();
            }
        }
        return new ExplorerNode(name, p.toString(), dir, type, playable, size, modified, streamUrl, posterUrl, year, genres, vote, mediaTypeDetail);
    }

    private Path resolveAllowed(User user, String rawPath) {
        return pathAccessValidator.resolveWithinRoots(user, rawPath);
    }
}