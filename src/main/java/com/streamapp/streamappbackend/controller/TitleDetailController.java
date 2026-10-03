package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.service.TitleDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tmdb")
public class TitleDetailController {

    @Autowired
    private TitleDetailsService titleDetailsService;

    /**
     * Busca un título por query y devuelve un DTO con los detalles.
     * Parámetro de consulta: q=título a buscar (se limpiará automáticamente).
     * Retorna 204 si no encuentra nada, para no ensuciar la consola del navegador con 404.
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam("q") String query,
                                    @RequestParam(value = "path", required = false) String path) {
        var result = titleDetailsService.searchAndFormat(query);
        if (result == null) {
            return ResponseEntity.ok(titleDetailsService.basicDetails(query, path));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/local-image")
    public ResponseEntity<Resource> localImage(@RequestParam("path") String mediaPath,
                                               @RequestParam("kind") String kind) {
        if (!java.util.Set.of("poster", "folder", "fanart", "backdrop").contains(kind)) {
            return ResponseEntity.badRequest().build();
        }
        java.nio.file.Path media = java.nio.file.Path.of(mediaPath).toAbsolutePath().normalize();
        java.nio.file.Path directory = media.getParent();
        if (directory == null) return ResponseEntity.notFound().build();
        java.util.List<String> names = java.util.List.of(kind + ".jpg", kind + ".jpeg", kind + ".png");
        for (String name : names) {
            java.nio.file.Path candidate = directory.resolve(name).normalize();
            if (candidate.getParent().equals(directory) && java.nio.file.Files.isRegularFile(candidate)) {
                Resource resource = new FileSystemResource(candidate);
                MediaType mediaType = name.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
                return ResponseEntity.ok().contentType(mediaType).body(resource);
            }
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Obtiene el detalle de una película por su ID de TMDB.
     */
    @GetMapping("/detail")
    public ResponseEntity<?> detail(@RequestParam("tmdbId") int tmdbId) {
        var result = titleDetailsService.getMovieDetails(tmdbId);
        if (result == null) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "No se pudieron obtener los detalles."));
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Igual que detail, pero para series de TV.
     */
    @GetMapping("/series")
    public ResponseEntity<?> seriesDetail(@RequestParam("tmdbId") int tmdbId) {
        var result = titleDetailsService.getSeriesDetails(tmdbId);
        if (result == null) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "No se pudieron obtener los detalles de la serie."));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/season")
    public ResponseEntity<?> seasonEpisodes(@RequestParam("tmdbId") int tmdbId, @RequestParam("season") int season) {
        var eps = titleDetailsService.getSeasonEpisodes(tmdbId, season);
        return ResponseEntity.ok(eps);
    }

    @GetMapping("/search-list")
    public ResponseEntity<?> searchList(@RequestParam("q") String query) {
        if (!titleDetailsService.isConfigured()) {
            return ResponseEntity.status(503).body(java.util.Map.of(
                    "error", "TMDB_NOT_CONFIGURED",
                    "message", "TMDB_API_KEY no está configurada en el backend."));
        }
        var list = titleDetailsService.searchCandidates(query);
        return ResponseEntity.ok(list);
    }
}