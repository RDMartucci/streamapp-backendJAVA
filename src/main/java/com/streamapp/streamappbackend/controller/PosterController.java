package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.entity.MediaItem;
import com.streamapp.streamappbackend.exception.NotFoundException;
import com.streamapp.streamappbackend.service.TmdbService;
import com.streamapp.streamappbackend.service.explorer.TitleParser;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/poster")
public class PosterController {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TmdbService tmdbService;

    @Autowired
    private TitleParser titleParser;

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
}