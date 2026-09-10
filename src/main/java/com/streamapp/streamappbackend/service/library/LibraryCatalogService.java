package com.streamapp.streamappbackend.service.library;

import com.streamapp.streamappbackend.dto.LibraryScanResult;
import com.streamapp.streamappbackend.entity.MediaItem;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.repository.MediaItemRepository;
import com.streamapp.streamappbackend.service.explorer.MediaTypeResolver;
import com.streamapp.streamappbackend.service.explorer.TitleParser;
import com.streamapp.streamappbackend.service.explorer.UserRootsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LibraryCatalogService {

    private final UserRootsService userRootsService;
    private final MediaTypeResolver mediaTypeResolver;
    private final TitleParser titleParser;
    private final MediaItemRepository mediaItemRepository;

    public LibraryCatalogService(UserRootsService userRootsService,
                                 MediaTypeResolver mediaTypeResolver,
                                 TitleParser titleParser,
                                 MediaItemRepository mediaItemRepository) {
        this.userRootsService = userRootsService;
        this.mediaTypeResolver = mediaTypeResolver;
        this.titleParser = titleParser;
        this.mediaItemRepository = mediaItemRepository;
    }

    @Transactional
    public LibraryScanResult scan(User user) {
        int indexed = 0;
        int updated = 0;
        int skipped = 0;

        for (String root : userRootsService.getRoots(user)) {
            Path rootPath = Path.of(root);
            if (!Files.isDirectory(rootPath)) {
                skipped++;
                continue;
            }
            try (var paths = Files.walk(rootPath)) {
                var iterator = paths.filter(Files::isRegularFile).iterator();
                while (iterator.hasNext()) {
                    Path path = iterator.next();
                    if (mediaTypeResolver.resolveType(path.getFileName().toString()) == null) {
                        skipped++;
                        continue;
                    }
                    boolean exists = mediaItemRepository.findByPath(path.toString()).isPresent();
                    upsert(path);
                    if (exists) {
                        updated++;
                    } else {
                        indexed++;
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo escanear la biblioteca: " + root, e);
            }
        }
        return new LibraryScanResult(indexed, updated, skipped);
    }

    @Transactional(readOnly = true)
    public List<MediaItem> list(User user) {
        List<MediaItem> result = new ArrayList<>();
        for (String root : userRootsService.getRoots(user)) {
            Path rootPath = Path.of(root).toAbsolutePath().normalize();
            String rootValue = rootPath.toString();
            mediaItemRepository.findByPathPrefix(rootValue).stream()
                    .filter(item -> isWithinRoot(item.getPath(), rootPath))
                    .forEach(result::add);
        }
        return result.stream()
                .distinct()
                .sorted(Comparator.comparing(MediaItem::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void upsert(Path path) throws IOException {
        String normalizedPath = path.toAbsolutePath().normalize().toString();
        MediaItem item = mediaItemRepository.findByPath(normalizedPath).orElseGet(MediaItem::new);
        item.setTitle(titleParser.clean(path.getFileName().toString()));
        item.setPath(normalizedPath);
        item.setSourceType(MediaItem.SourceType.LOCAL);
        item.setMediaType(mediaTypeResolver.resolveType(path.getFileName().toString()));
        item.setContentType(contentType(path));
        item.setSizeBytes(Files.size(path));
        item.setLastModified(Files.getLastModifiedTime(path).toInstant());
        mediaItemRepository.save(item);
    }

    private String contentType(Path path) throws IOException {
        String detected = Files.probeContentType(path);
        return detected == null ? "application/octet-stream" : detected;
    }

    private boolean isWithinRoot(String rawPath, Path root) {
        try {
            return Path.of(rawPath).toAbsolutePath().normalize().startsWith(root);
        } catch (Exception ignored) {
            return false;
        }
    }
}
