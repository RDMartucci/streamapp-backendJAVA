package com.streamapp.streamappbackend.service.library;

import com.streamapp.streamappbackend.dto.LibraryScanResult;
import com.streamapp.streamappbackend.dto.LibraryPageDto;
import com.streamapp.streamappbackend.dto.LibraryFilterOptionsDto;
import com.streamapp.streamappbackend.dto.MediaItemDto;
import com.streamapp.streamappbackend.dto.TitleDetailDto;
import com.streamapp.streamappbackend.entity.MediaItem;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.repository.MediaItemRepository;
import com.streamapp.streamappbackend.service.TitleDetailsService;
import com.streamapp.streamappbackend.service.media.MediaItemDtoMapper;
import com.streamapp.streamappbackend.service.explorer.MediaTypeResolver;
import com.streamapp.streamappbackend.service.explorer.TitleParser;
import com.streamapp.streamappbackend.service.explorer.UserRootsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Service
public class LibraryCatalogService {

    private static final Logger log = LoggerFactory.getLogger(LibraryCatalogService.class);

    private final UserRootsService userRootsService;
    private final MediaTypeResolver mediaTypeResolver;
    private final TitleParser titleParser;
    private final MediaItemRepository mediaItemRepository;
    private final MediaItemDtoMapper mediaItemDtoMapper;
    private final TitleDetailsService titleDetailsService;
    private final ConcurrentHashMap<String, AtomicBoolean> scanCancellations = new ConcurrentHashMap<>();

    public LibraryCatalogService(UserRootsService userRootsService,
                                 MediaTypeResolver mediaTypeResolver,
                                 TitleParser titleParser,
                                 MediaItemRepository mediaItemRepository,
                                 MediaItemDtoMapper mediaItemDtoMapper,
                                 TitleDetailsService titleDetailsService) {
        this.userRootsService = userRootsService;
        this.mediaTypeResolver = mediaTypeResolver;
        this.titleParser = titleParser;
        this.mediaItemRepository = mediaItemRepository;
        this.mediaItemDtoMapper = mediaItemDtoMapper;
        this.titleDetailsService = titleDetailsService;
    }

    public LibraryScanResult scan(User user) {
        String username = user.getUsername();
        AtomicBoolean cancellation = new AtomicBoolean();
        if (scanCancellations.putIfAbsent(username, cancellation) != null) {
            throw new IllegalStateException("Ya hay una actualización de biblioteca en curso");
        }
        int indexed = 0;
        int updated = 0;
        int skipped = 0;

        try {
            for (String root : userRootsService.getRoots(user)) {
                if (cancellation.get()) break;
                Path rootPath = Path.of(root);
                if (!Files.isDirectory(rootPath)) {
                    skipped++;
                    continue;
                }
                try {
                    final int[] counters = { indexed, updated, skipped };
                    Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            return cancellation.get() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                            if (cancellation.get()) return FileVisitResult.TERMINATE;
                            if (!attrs.isRegularFile() || mediaTypeResolver.resolveType(path.getFileName().toString()) == null) {
                                counters[2]++;
                                return FileVisitResult.CONTINUE;
                            }
                            try {
                                String normalizedPath = normalizePath(path);
                                boolean exists = mediaItemRepository.findByNormalizedPath(normalizedPath).isPresent();
                                upsert(path);
                                if (exists) {
                                    counters[1]++;
                                } else {
                                    counters[0]++;
                                }
                            } catch (IOException | RuntimeException e) {
                                counters[2]++;
                                log.warn("No se pudo indexar el archivo {}", path, e);
                            }
                            return cancellation.get() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            counters[2]++;
                            return cancellation.get() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                            return cancellation.get() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                        }
                    });
                    indexed = counters[0];
                    updated = counters[1];
                    skipped = counters[2];
                } catch (IOException | SecurityException e) {
                    throw new IllegalStateException("No se pudo escanear la biblioteca: " + root, e);
                }
            }
            if (!cancellation.get()) enrichMissingMetadata(user, cancellation);
            return new LibraryScanResult(indexed, updated, skipped, cancellation.get());
        } finally {
            scanCancellations.remove(username, cancellation);
        }
    }

    public boolean cancelScan(User user) {
        AtomicBoolean cancellation = scanCancellations.get(user.getUsername());
        if (cancellation == null) return false;
        cancellation.set(true);
        return true;
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

    @Transactional(readOnly = true)
    public LibraryFilterOptionsDto filterOptions(User user) {
        List<MediaItem> catalogItems = list(user);
        TreeSet<String> actors = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> directors = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<String> genres = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        TreeSet<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        TreeSet<Integer> stars = new TreeSet<>(Comparator.reverseOrder());

        for (MediaItem item : catalogItems) {
            addDelimitedValues(actors, item.getCast());
            addDelimitedValues(directors, item.getDirectors());
            addDelimitedValues(genres, item.getGenres());
            if (item.getYear() != null) years.add(item.getYear());
            if (item.getVoteAverage() != null) {
                stars.add((int) Math.round(item.getVoteAverage() / 2.0));
            }
        }
        return new LibraryFilterOptionsDto(
                List.copyOf(actors), List.copyOf(directors), List.copyOf(years),
                List.copyOf(genres), List.copyOf(stars));
    }

    @Transactional(readOnly = true)
    public LibraryPageDto page(User user, int page, int size, String query, String type,
                               String actor, String director, Integer year, String genre, Double stars) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Specification<MediaItem> specification = withinUserRoots(user);
        if (query != null && !query.isBlank()) {
            String term = "%" + query.trim().toLowerCase() + "%";
            specification = specification.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), term),
                    cb.like(cb.lower(root.get("path")), term)));
        }
        if (actor != null && !actor.isBlank()) {
            specification = specification.and((root, q, cb) -> {
                var normalizedCast = cb.function("replace", String.class,
                        cb.lower(root.get("cast")), cb.literal(", "), cb.literal(","));
                var delimitedCast = cb.concat(cb.concat(",", normalizedCast), ",");
                return cb.like(delimitedCast, "%," + actor.trim().toLowerCase() + ",%");
            });
        }
        if (director != null && !director.isBlank()) {
            specification = specification.and((root, q, cb) -> {
                var normalizedDirectors = cb.function("replace", String.class,
                        cb.lower(root.get("directors")), cb.literal(", "), cb.literal(","));
                var delimitedDirectors = cb.concat(cb.concat(",", normalizedDirectors), ",");
                return cb.like(delimitedDirectors, "%," + director.trim().toLowerCase() + ",%");
            });
        }
        if (year != null) {
            specification = specification.and((root, q, cb) ->
                    cb.equal(root.get("year"), year));
        }
        if (genre != null && !genre.isBlank()) {
            specification = specification.and((root, q, cb) -> {
                var normalizedGenres = cb.function("replace", String.class,
                        cb.lower(root.get("genres")), cb.literal(", "), cb.literal(","));
                var delimitedGenres = cb.concat(cb.concat(",", normalizedGenres), ",");
                return cb.like(delimitedGenres, "%," + genre.trim().toLowerCase() + ",%");
            });
        }
        if (stars != null) {
            specification = specification.and((root, q, cb) ->
                    cb.greaterThanOrEqualTo(root.<Double>get("voteAverage"), (stars - 0.5) * 2));
        }
        if (type != null && !type.isBlank() && !"all".equalsIgnoreCase(type)) {
            if ("audio".equalsIgnoreCase(type)) {
                specification = specification.and((root, q, cb) ->
                        cb.equal(root.get("mediaType"), MediaItem.MediaType.AUDIO));
            } else if ("movie".equalsIgnoreCase(type) || "series".equalsIgnoreCase(type)) {
                specification = specification.and((root, q, cb) ->
                        cb.equal(cb.lower(root.get("mediaTypeDetail")), type.toLowerCase()));
            }
        }
        Page<MediaItem> result = mediaItemRepository.findAll(specification,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "title")));
        List<MediaItem> catalogItems = list(user);
        long movieCount = catalogItems.stream()
                .filter(item -> isDetailType(item, "movie"))
                .count();
        long seriesCount = catalogItems.stream()
                .filter(item -> isDetailType(item, "series"))
                .count();
        long audioCount = catalogItems.stream()
                .filter(item -> item.getMediaType() == MediaItem.MediaType.AUDIO)
                .count();
        return new LibraryPageDto(
                result.getContent().stream().map(item -> mediaItemDtoMapper.toDto(user, item)).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                movieCount, seriesCount, audioCount);
    }

    private void enrichMissingMetadata(User user, AtomicBoolean cancellation) {
        for (MediaItem item : list(user)) {
            if (cancellation.get()) return;
            if (item.getMediaType() != MediaItem.MediaType.VIDEO
                    || (hasText(item.getCast())
                    && hasText(item.getDirectors())
                    && item.getYear() != null
                    && hasText(item.getGenres())
                    && item.getVoteAverage() != null)) {
                continue;
            }
            try {
                TitleDetailDto details = null;
                if (item.getTmdbId() != null && item.getMediaTypeDetail() != null) {
                    details = "series".equalsIgnoreCase(item.getMediaTypeDetail())
                            ? titleDetailsService.getSeriesDetails(item.getTmdbId().intValue(), cancellation::get)
                            : titleDetailsService.getMovieDetails(item.getTmdbId().intValue(), cancellation::get);
                }
                if (details == null) {
                    if (cancellation.get()) return;
                    details = titleDetailsService.searchAndFormat(item.getTitle(), cancellation::get);
                }
                if (details == null) {
                    details = titleDetailsService.basicDetails(item.getTitle(), item.getPath());
                }
                if (cancellation.get()) return;
                boolean changed = false;
                if (details.year() != null && !details.year().equals(item.getYear())) {
                    item.setYear(details.year());
                    changed = true;
                }
                if (hasText(details.genres()) && !details.genres().equals(item.getGenres())) {
                    item.setGenres(details.genres());
                    changed = true;
                }
                if (hasText(details.cast()) && !details.cast().equals(item.getCast())) {
                    item.setCast(details.cast());
                    changed = true;
                }
                if (hasText(details.directors()) && !details.directors().equals(item.getDirectors())) {
                    item.setDirectors(details.directors());
                    changed = true;
                }
                if (details.voteAverage() != null && !details.voteAverage().equals(item.getVoteAverage())) {
                    item.setVoteAverage(details.voteAverage());
                    changed = true;
                }
                if (details.tmdbId() != null && !details.tmdbId().equals(item.getTmdbId())) {
                    item.setTmdbId(details.tmdbId());
                    changed = true;
                }
                if (details.mediaType() != null && !details.mediaType().equals(item.getMediaTypeDetail())) {
                    item.setMediaTypeDetail(details.mediaType());
                    changed = true;
                }
                if (hasText(details.posterUrl()) && !details.posterUrl().equals(item.getPosterUrl())) {
                    item.setPosterUrl(details.posterUrl());
                    changed = true;
                }
                if (hasText(details.overview()) && !details.overview().equals(item.getOverview())) {
                    item.setOverview(details.overview());
                    changed = true;
                }
                if (changed) {
                    item.setMetadataProvider(details.tmdbId() != null ? "tmdb" : "local");
                    item.setMetadataUpdatedAt(Instant.now());
                    mediaItemRepository.save(item);
                }
            } catch (RuntimeException e) {
                log.warn("No se pudo completar la metadata de {}", item.getPath(), e);
            }
        }
    }

    private void addDelimitedValues(TreeSet<String> values, String rawValues) {
        if (!hasText(rawValues)) return;
        Stream.of(rawValues.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .forEach(values::add);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isDetailType(MediaItem item, String expected) {
        if (item.getMediaType() != MediaItem.MediaType.VIDEO) {
            return false;
        }
        String detail = item.getMediaTypeDetail();
        if (detail == null || detail.isBlank()) {
            detail = titleParser.mediaTypeDetail(item.getPath(), true);
        }
        return expected.equalsIgnoreCase(detail);
    }

    private Specification<MediaItem> withinUserRoots(User user) {
        List<String> roots = userRootsService.getRoots(user).stream()
                .map(root -> normalizePath(Path.of(root)) + "/")
                .toList();
        return (root, query, cb) -> {
            if (roots.isEmpty()) {
                return cb.disjunction();
            }
            return cb.or(roots.stream()
                    .map(prefix -> cb.like(
                            cb.lower(cb.function("replace", String.class, root.get("path"), cb.literal("\\"), cb.literal("/"))),
                            prefix.toLowerCase() + "%"))
                    .toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private void upsert(Path path) throws IOException {
        String normalizedPath = normalizePath(path);
        MediaItem item = mediaItemRepository.findByPath(normalizedPath)
                .orElseGet(() -> mediaItemRepository.findByNormalizedPath(normalizedPath)
                        .orElseGet(MediaItem::new));
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
