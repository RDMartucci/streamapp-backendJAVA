package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.dto.FsEntry;
import com.streamapp.streamappbackend.exception.ForbiddenException;
import com.streamapp.streamappbackend.exception.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/fs")
public class FileSystemController {

    @GetMapping("/roots")
    public List<FsEntry> roots() {
        return browserRoots().entrySet().stream()
            .map(root -> new FsEntry(root.getKey(), root.getValue().toString().replace('\\', '/')))
                .toList();
    }

    @GetMapping("/list")
    public List<FsEntry> list(@RequestParam("path") String path) {
        Path dir = resolveBrowserPath(path);
        if (!Files.exists(dir)) {
            throw new NotFoundException("La ruta no existe: " + path);
        }
        if (!Files.isDirectory(dir)) {
            throw new NotFoundException("La ruta no es un directorio: " + path);
        }
        List<FsEntry> entries = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        String fullPath = p.toAbsolutePath().toString().replace('\\', '/');
                        entries.add(new FsEntry(name, fullPath));
                    });
        } catch (Exception e) {
            throw new NotFoundException("No se pudo leer el directorio: " + path);
        }
        return entries;
    }

    private Path resolveBrowserPath(String rawPath) {
        final Path requested;
        try {
            requested = Paths.get(rawPath).toAbsolutePath().normalize();
        } catch (Exception e) {
            throw new ForbiddenException("Ruta inválida");
        }

        final Path realRequested;
        try {
            realRequested = requested.toRealPath();
        } catch (IOException e) {
            throw new NotFoundException("La ruta no existe: " + rawPath);
        }

        for (Path allowedRoot : browserRoots().values()) {
            try {
                if (realRequested.startsWith(allowedRoot.toRealPath())) {
                    return realRequested;
                }
            } catch (IOException ignored) {
                // La carpeta especial puede desaparecer mientras se consulta.
            }
        }
        throw new ForbiddenException("Solo se pueden examinar las carpetas personales permitidas");
    }

    private Map<String, Path> browserRoots() {
        Path home = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();
        Map<String, Path> candidates = new LinkedHashMap<>();
        addIfDirectory(candidates, "Descargas", home.resolve("Downloads"));
        addIfDirectory(candidates, "Descargas", home.resolve("Descargas"));
        addIfDirectory(candidates, "Videos", home.resolve("Videos"));
        addIfDirectory(candidates, "Música", home.resolve("Music"));
        addIfDirectory(candidates, "Música", home.resolve("Música"));
        addIfDirectory(candidates, "Escritorio", home.resolve("Desktop"));
        addIfDirectory(candidates, "Escritorio", home.resolve("Escritorio"));
        addIfDirectory(candidates, "Documentos", home.resolve("Documents"));
        addIfDirectory(candidates, "Documentos", home.resolve("Documentos"));
        addIfDirectory(candidates, "Imágenes", home.resolve("Pictures"));
        addIfDirectory(candidates, "Imágenes", home.resolve("Imágenes"));
        return candidates;
    }

    private void addIfDirectory(Map<String, Path> candidates, String name, Path path) {
        if (Files.isDirectory(path)) {
            candidates.putIfAbsent(name + " (" + path.getFileName() + ")", path);
        }
    }
}
