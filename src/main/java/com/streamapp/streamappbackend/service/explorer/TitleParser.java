package com.streamapp.streamappbackend.service.explorer;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class TitleParser {

    // Patrones globales (cualquier posición)
    private static final Pattern YEAR_ANY = Pattern.compile("\\b(?:19|20)\\d{2}\\b");
    private static final Pattern RESOLUTION = Pattern.compile("\\b\\d{3,4}p\\b|\\b4K\\b|\\b8K\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODEC = Pattern.compile("\\b(?:x264|x265|H\\.?264|HEVC|AVC|HVEC|H265)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUALITY = Pattern.compile("\\b(?:WEBRip|WEB-DL|WEB|BluRay|Blu-Ray|BRRip|BDRip|DVDRip|HDR|HDR10|HDTV|PPV|Remux)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AUDIO = Pattern.compile("\\b(?:DDP\\d+\\.\\d+|DD5\\.1|AC3|AAC|DTS(?:-HD)?|Atmos|TrueHD|FLAC|Opus|MP3)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BITDEPTH = Pattern.compile("\\b\\d+Bit\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LANG = Pattern.compile("\\b(?:Dual|Lat|Esp|Eng|Multi|Subs?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_SUFFIX = Pattern.compile("[-_][A-Za-z0-9]+$");
    private static final Pattern EXTENSION = Pattern.compile("\\.[a-z0-9]+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_BRACKETS = Pattern.compile("[\\[\\]()]+");

    public String clean(String filename) {
        if (filename == null || filename.isBlank()) return filename;
        String name = filename.trim();

        // 1. Quitar extensión
        name = EXTENSION.matcher(name).replaceFirst("");

        // 2. Quitar sufijo de grupo tipo "-NeoNoir" al final
        name = GROUP_SUFFIX.matcher(name).replaceFirst("");

        // 3. Si hay año, truncar todo desde el año (el título suele estar antes del año)
        var yearMatcher = YEAR_ANY.matcher(name);
        if (yearMatcher.find()) {
            name = name.substring(0, yearMatcher.start());
        } else {
            // Sin año: quitar tags globales
            name = RESOLUTION.matcher(name).replaceAll(" ");
            name = CODEC.matcher(name).replaceAll(" ");
            name = QUALITY.matcher(name).replaceAll(" ");
            name = AUDIO.matcher(name).replaceAll(" ");
            name = BITDEPTH.matcher(name).replaceAll(" ");
            name = LANG.matcher(name).replaceAll(" ");
        }

        // 4. Limpiar separadores y compactar
        name = TAG_BRACKETS.matcher(name).replaceAll(" ");
        name = name.replace('.', ' ').replace('_', ' ').replace('-', ' ');
        name = name.replaceAll("\\s+", " ").trim();

        // 5. Si quedó solo basura tipo base64 (muy largo sin vocales o sin espacios), devolver vacío para no buscar
        if (name.length() > 35 && !name.contains(" ") && name.matches("^[A-Za-z0-9+/=]+$")) {
            return "";
        }

        return name.isEmpty() ? "" : name;
    }
}
