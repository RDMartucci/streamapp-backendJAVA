package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.entity.MediaItem;
import com.streamapp.streamappbackend.entity.User;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.repository.UserRepository;
import com.streamapp.streamappbackend.service.TmdbService;
import com.streamapp.streamappbackend.service.explorer.TitleParser;
import com.streamapp.streamappbackend.service.media.MediaItemService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;

@RestController
@RequestMapping("/api/poster")
public class PosterController {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TmdbService tmdbService;

    @Autowired
    private TitleParser titleParser;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaItemService mediaItemService;

    @PostConstruct
    public void init() {
        if (entityManager == null) {
            // Fallback: intentar obtener el entityManager del contexto spring
            // (esto es solo para evitar errores en pruebas sin spring full context)
        }
    }

    /**
     * Busca un poster en TMDB por título.
     * Parámetro de consulta: q=título a buscar
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> search(@RequestParam("q") String title) {
        String posterUrl = tmdbService.searchPoster(title);
        java.util.Map<String, String> result = new java.util.HashMap<>();
        if (posterUrl != null) {
            result.put("posterUrl", posterUrl);
            result.put("found", "true");
        } else {
            result.put("found", "false");
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Asigna un posterUrl manual a un MediaItem.
     * Body JSON: { "posterUrl": "https://..." }
     */
    @PutMapping("/media/{id}")
    @Transactional
    public ResponseEntity<MediaItem> setPoster(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String posterUrl = body.get("posterUrl");
        if (posterUrl == null || posterUrl.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        MediaItem item = entityManager.find(MediaItem.class, id);
        if (item == null) {
            throw new NotFoundException("MediaItem no encontrado id=" + id);
        }

        item.setPosterUrl(posterUrl);
        entityManager.merge(item);
        return ResponseEntity.ok(item);
    }

    /**
     * Quita el poster de un MediaItem (lo pone en null).
     */
    @DeleteMapping("/media/{id}")
    @Transactional
    public ResponseEntity<Void> removePoster(@PathVariable Long id) {
        MediaItem item = entityManager.find(MediaItem.class, id);
        if (item == null) {
            throw new NotFoundException("MediaItem no encontrado id=" + id);
        }

        item.setPosterUrl(null);
        entityManager.merge(item);
        return ResponseEntity.ok().build();
    }

    /**
     * Guarda el posterUrl para un archivo por su path absoluto.
     * Body JSON: { "path": "C:/media/...mkv", "posterUrl": "https://..." }
     * Si el MediaItem no existe aún, lo crea.
     */
    @PutMapping("/by-path")
    @Transactional
    public ResponseEntity<Map<String, String>> setPosterByPath(Authentication authentication, @RequestBody Map<String, String> body) {
        String path = body.get("path");
        String posterUrl = body.get("posterUrl");
        if (path == null || path.isBlank() || posterUrl == null || posterUrl.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // Normalizar a forward slashes y lowercase para consistencia en BD
        String normalized = path.replace('\\', '/');
        // Buscar por path normalizado (case-insensitive) y también con backslashes por compatibilidad
        var query = entityManager.createNativeQuery(
            "SELECT * FROM media_items WHERE LOWER(REPLACE(path, '\\\\', '/')) = :path OR LOWER(path) = :path2", MediaItem.class);
        query.setParameter("path", normalized);
        query.setParameter("path2", path);
        @SuppressWarnings("unchecked")
        var list = query.getResultList();
        MediaItem item;
        if (list.isEmpty()) {
            try {
                if (authentication != null) {
                    User user = userRepository.findByUsername(authentication.getName()).orElse(null);
                    if (user != null) {
                        Path p = Paths.get(path);
                        item = mediaItemService.findOrCreate(p);
                    } else {
                        return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
                    }
                } else {
                    return ResponseEntity.status(404).body(Map.of("error", "MediaItem no encontrado para path: " + path));
                }
            } catch (Exception e) {
                System.err.println("Error creating MediaItem for path " + path + ": " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
            }
        } else {
            item = (MediaItem) list.get(0);
        }
        try {
            item.setPosterUrl(posterUrl);
            String yearStr = body.get("year");
            if (yearStr != null && !yearStr.isBlank()) try { item.setYear(Integer.parseInt(yearStr)); } catch (NumberFormatException ignored) {}
            if (body.get("genres") != null) item.setGenres(body.get("genres"));
            String voteStr = body.get("voteAverage");
            if (voteStr != null && !voteStr.isBlank()) try { item.setVoteAverage(Double.parseDouble(voteStr)); } catch (NumberFormatException ignored) {}
            if (body.get("titleOriginal") != null) item.setTitleOriginal(body.get("titleOriginal"));
            if (body.get("overview") != null) item.setOverview(body.get("overview"));
            if (body.get("mediaType") != null) item.setMediaTypeDetail(body.get("mediaType"));
            entityManager.merge(item);
            entityManager.flush();
            return ResponseEntity.ok(Map.of("posterUrl", posterUrl));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(409).body(Map.of("error", "Conflicto de datos: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error updating poster for path " + path + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Error interno: " + e.getMessage()));
        }
    }
}