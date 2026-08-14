package com.streamapp.streamappbackend.service.explorer;

import com.streamapp.streamappbackend.entity.MediaItem;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class MediaTypeResolver {

    private static final Set<String> VIDEO =
            Set.of("mp4", "mkv", "avi", "mov", "webm", "m4v", "3gp", "mpg", "mpeg", "ts", "ogv");

    private static final Set<String> AUDIO =
            Set.of("mp3", "flac", "wav", "ogg", "m4a", "aac", "wma", "opus", "aiff", "aif");

    private static final Set<String> SUBTITLES =
            Set.of("srt", "vtt");

    /**
     * Devuelve el tipo media de una extensión, o null si no es un archivo
     * de audio, video o subtítulo.
     */
    public MediaItem.MediaType resolveType(String filename) {
        String ext = extensionOf(filename);
        if (ext == null) {
            return null;
        }
        if (VIDEO.contains(ext)) {
            return MediaItem.MediaType.VIDEO;
        }
        if (AUDIO.contains(ext)) {
            return MediaItem.MediaType.AUDIO;
        }
        if (SUBTITLES.contains(ext)) {
            return null; // los subtítulos no se tratan como media reproducible
        }
        return null;
    }

    public boolean isSubtitle(String filename) {
        String ext = extensionOf(filename);
        return ext != null && SUBTITLES.contains(ext);
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return null;
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return null;
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }
}