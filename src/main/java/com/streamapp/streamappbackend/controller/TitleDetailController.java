package com.streamapp.streamappbackend.controller;

import com.streamapp.streamappbackend.service.TitleDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tmdb")
public class TitleDetailController {

    @Autowired
    private TitleDetailsService titleDetailsService;

    /**
     * Busca un título por query y devuelve un DTO con los detalles.
     * Parámetro de consulta: q=título a buscar (se limpiará automáticamente).
     */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam("q") String query) {
        var result = titleDetailsService.searchAndFormat(query);
        if (result == null) {
            return ResponseEntity.status(404).body(java.util.Map.of("error", "No se encontraron datos de TMDB para: " + query));
        }
        return ResponseEntity.ok(result);
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
}