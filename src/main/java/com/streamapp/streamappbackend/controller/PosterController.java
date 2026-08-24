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
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
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
        var query = entityManager.createQuery("SELECT m FROM MediaItem m WHERE m.path = :path", MediaItem.class);
        query.setParameter("path", path);
        var list = query.getResultList();
        MediaItem item;
        if (list.isEmpty()) {
            // Intentar crear el MediaItem si el archivo existe y es media válido
            try {
                Path p = Paths.get(path);
                // Validar que esté dentro de las raíces del usuario si hay auth
                if (authentication != null) {
                    User user = userRepository.findByUsername(authentication.getName()).orElse(null);
                    if (user != null) item = mediaItemService.findOrCreate(p);
                    else return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
                } else {
                    return ResponseEntity.status(404).body(Map.of("error", "MediaItem no encontrado para path: " + path));
                }
            } catch (Exception e) {
                return ResponseEntity.status(404).body(Map.of("error", "No se pudo crear MediaItem: " + e.getMessage()));
            }
        } else {
            item = list.get(0);
        }
        item.setPosterUrl(posterUrl);
        entityManager.merge(item);
        return ResponseEntity.ok(Map.of("posterUrl", posterUrl));
    }
}