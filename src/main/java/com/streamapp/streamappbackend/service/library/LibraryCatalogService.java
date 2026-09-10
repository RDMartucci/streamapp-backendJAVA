package com.streamapp.streamappbackend.service.library;

import com.streamapp.streamappbackend.dto.LibraryScanResult;
import com.streamapp.streamappbackend.dto.MediaItemDto;
import com.streamapp.streamappbackend.entity.MediaItem;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.repository.MediaItemRepository;
import com.streamapp.streamappbackend.service.media.MediaItemDtoMapper;
import com.streamapp.streamappbackend.service.explorer.MediaTypeResolver;
import com.streamapp.streamappbackend.service.explorer.TitleParser;
import com.streamapp.streamappbackend.service.explorer.UserRootsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
    private final MediaItemDtoMapper mediaItemDtoMapper;

    public LibraryCatalogService(UserRootsService userRootsService,
                                 MediaTypeResolver mediaTypeResolver,
                                 TitleParser titleParser,
                                 MediaItemRepository mediaItemRepository,
                                 MediaItemDtoMapper mediaItemDtoMapper) {
        this.userRootsService = userRootsService;
        this.mediaTypeResolver = mediaTypeResolver;
        this.titleParser = titleParser;
        this.mediaItemRepository = mediaItemRepository;
        this.mediaItemDtoMapper = mediaItemDtoMapper;
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
            try {
                final int[] counters = { indexed, updated, skipped };
                Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                        if (!attrs.isRegularFile() || mediaTypeResolver.resolveType(path.getFileName().toString()) == null) {
                            counters[2]++;
                            return FileVisitResult.CONTINUE;
                        }
                        String normalizedPath = normalizePath(path);
                        boolean exists = mediaItemRepository.findByNormalizedPath(normalizedPath).isPresent();
                        try {
                            upsert(path);
                            if (exists) {
                                counters[1]++;
                            } else {
                                counters[0]++;
                            }
                        } catch (IOException | RuntimeException e) {
                            counters[2]++;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        counters[2]++;
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
                indexed = counters[0];
                updated = counters[1];
                skipped = counters[2];
            } catch (IOException | SecurityException e) {
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
            String rootValue = normalizePath(rootPath);
            mediaItemRepository.findByNormalizedPathPrefix(rootValue).stream()
                    .filter(item -> isWithinRoot(item.getPath(), rootPath))
                    .forEach(result::add);
        }
        return result.stream()
                .distinct()
                .sorted(Comparator.comparing(MediaItem::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MediaItemDto> listDtos(User user) {
        return list(user).stream()
                .map(item -> mediaItemDtoMapper.toDto(user, item))
                .toList();
    }

    private void upsert(Path path) throws IOException {
        String normalizedPath = normalizePath(path);
        MediaItem item = mediaItemRepository.findByNormalizedPath(normalizedPath).orElseGet(MediaItem::new);
        item.setTitle(titleParser.clean(path.getFileName().toString()));
        item.setPath(normalizedPath);
        item.setSourceType(MediaItem.SourceType.LOCAL);
        item.setMediaType(mediaTypeResolver.resolveType(path.getFileName().toString()));
        item.setMediaTypeDetail(titleParser.mediaTypeDetail(
                path.getFileName().toString(), item.getMediaType() == MediaItem.MediaType.VIDEO));
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
            return Path.of(rawPath.replace('/', java.io.File.separatorChar))
                    .toAbsolutePath().normalize().startsWith(root);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String normalizePath(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
