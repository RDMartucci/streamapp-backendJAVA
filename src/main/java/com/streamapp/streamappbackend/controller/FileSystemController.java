package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.dto.FsEntry;
import com.streamapp.streamappbackend.exception.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/fs")
public class FileSystemController {

    @GetMapping("/roots")
    public List<FsEntry> roots() {
        File[] roots = File.listRoots();
        if (roots == null || roots.length == 0) {
            return List.of();
        }
        List<FsEntry> entries = new ArrayList<>();
        for (File root : roots) {
            String path = root.getAbsolutePath().replace('\\', '/');
            entries.add(new FsEntry(path, path));
        }
        return entries;
    }

    @GetMapping("/list")
    public List<FsEntry> list(@RequestParam("path") String path) {
        Path dir = Paths.get(path);
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
}
