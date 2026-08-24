package com.streamapp.streamappbackend.service.explorer;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TitleParser {

    private static final Pattern YEAR_PATTERN = Pattern.compile(
            "\\(?\\b(?:19|20)\\d{2}\\)?\\s*$|\\b(?:19|20)\\d{2}\\s*$");

    private static final Pattern CODEC_PATTERNS = Pattern.compile(
            "\\b(?:HVEC|H264|x265|H\\.?264|x264|HEVC)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern RESOLUTION_PATTERNS = Pattern.compile(
            "\\b(?:1080p|720p|4K|2160p|1080i|576p|360p)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern QUALITY_PATTERNS = Pattern.compile(
            "\\b(?:BluRay|WEB-DL|DVDRip|HDR|HDTV|PPV|Streaming)\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern EXTENSION_PATTERN = Pattern.compile("\\.[a-z0-9]+$", Pattern.CASE_INSENSITIVE);

    /**
     * Limpia un nombre de archivo y extrae el título.
     * Ejemplos:
     * "Inception 2010 HVEC 1080p bla bla.mp4"  → "Inception"
     * "The.Matrix.1999.BluRay.1080p.DTS-HD.mkv" → "The Matrix"
     * "video.mp4" → "video"
     */
    public String clean(String filename) {
        if (filename == null || filename.isBlank()) {
            return filename;
        }

        String name = filename;

        // 1. Quitar extensión
        name = EXTENSION_PATTERN.matcher(name).replaceFirst("");

        // 2. Quitar year suelto o entre paréntesis al final
        name = YEAR_PATTERN.matcher(name).replaceFirst("").trim();

        // 3. Quitar codecs
        name = CODEC_PATTERNS.matcher(name).replaceFirst("").trim();

        // 4. Quitar resoluciones
        name = RESOLUTION_PATTERNS.matcher(name).replaceFirst("").trim();

        // 5. Quitar etiquetas de calidad
        name = QUALITY_PATTERNS.matcher(name).replaceFirst("").trim();

        // 6. Reemplazar puntos/guiones por espacios y compactar
        name = name.replace('.', ' ')
                 .replace('-', ' ')
                 .replaceAll("\\s+", " ")
                 .trim();

        return name.isEmpty() ? filename : name;
    }
}