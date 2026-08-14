package com.streamapp.streamappbackend.service.streaming;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sirve archivos de media soportando requests HTTP Range (seek), con
 * Accept-Ranges: bytes. Funciona con el reproductor interno (HTML5/ExoPlayer)
 * y con reproductores externos como VLC.
 *
 * El manejo del rango es manual (write-in-stream) para no depender del
 * converter de ResourceRegion y poder usar cualquier content-type.
 */
@Service
public class RangeStreaming implements StreamingAdapter {

    private static final Pattern SINGLE_BYTE_RANGE = Pattern.compile("bytes=(\\d*)-(\\d*)");

    @Override
    public ResponseEntity<StreamingResponseBody> stream(Path resolvedPath, String rangeHeader) {
        long fileSize;
        String contentType;
        try {
            fileSize = Files.size(resolvedPath);
            contentType = probeContentType(resolvedPath.toString());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo acceder al archivo", e);
        }

        long start = 0;
        long end = fileSize - 1;
        boolean partial = false;

        Range range = parse(rangeHeader, fileSize);
        if (range != null) {
            start = range.start();
            end = range.end();
            partial = true;
        }

        final long from = start;
        final long to = end;
        long length = to - from + 1;

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        headers.setContentType(MediaType.parseMediaType(contentType));
        if (partial) {
            headers.set(HttpHeaders.CONTENT_RANGE,
                    "bytes " + from + "-" + to + "/" + fileSize);
        }

        StreamingResponseBody body = outputStream -> {
            try {
                writeRange(resolvedPath, from, to, outputStream);
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Error al escribir el rango", e);
            }
        };

        if (partial) {
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentLength(length)
                    .body(body);
        }
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(fileSize)
                .body(body);
    }

    private void writeRange(Path file, long start, long end, OutputStream out) throws Exception {
        try (InputStream in = Files.newInputStream(file)) {
            long skip = start;
            long remaining = end - start + 1;
            long skipped = 0;
            while (skip > 0) {
                skipped = in.skip(skip);
                if (skipped == 0) {
                    if (in.read() == -1) {
                        return;
                    }
                    skipped = 1;
                }
                skip -= skipped;
            }
            byte[] buffer = new byte[8192];
            int read;
            while (remaining > 0 && (read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                out.write(buffer, 0, read);
                remaining -= read;
            }
        }
    }

    /**
     * Parsea un header Range de un solo rango (bytes=A-B). Devuelve null si no
     * hay rango válido. Soporta: bytes=100- (a fin), bytes=-500 (últimos 500),
     * bytes=0-99.
     */
    Range parse(String header, long fileSize) {
        if (header == null || header.isBlank()) {
            return null;
        }
        Matcher m = SINGLE_BYTE_RANGE.matcher(header.trim());
        if (!m.matches()) {
            return null;
        }
        String sRaw = m.group(1);
        String eRaw = m.group(2);

        long start;
        long end;
        if (sRaw.isEmpty()) {
            // Suffix range: bytes=-N → últimos N bytes
            long suffix = Long.parseLong(eRaw);
            start = Math.max(0, fileSize - suffix);
            end = fileSize - 1;
        } else {
            start = Long.parseLong(sRaw);
            end = eRaw.isEmpty() ? fileSize - 1 : Long.parseLong(eRaw);
        }
        if (start >= fileSize) {
            return null;
        }
        if (end >= fileSize) {
            end = fileSize - 1;
        }
        if (start > end) {
            return null;
        }
        return new Range(start, end);
    }

    private record Range(long start, long end) {
    }

    private String probeContentType(String filename) {
        String name = filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp4")) return "video/mp4";
        if (name.endsWith(".mkv")) return "video/x-matroska";
        if (name.endsWith(".webm")) return "video/webm";
        if (name.endsWith(".mov")) return "video/quicktime";
        if (name.endsWith(".avi")) return "video/x-msvideo";
        if (name.endsWith(".mp3")) return "audio/mpeg";
        if (name.endsWith(".flac")) return "audio/flac";
        if (name.endsWith(".wav")) return "audio/wav";
        if (name.endsWith(".ogg")) return "audio/ogg";
        if (name.endsWith(".m4a")) return "audio/mp4";
        if (name.endsWith(".aac")) return "audio/aac";
        if (name.endsWith(".wma")) return "audio/x-ms-wma";
        return "application/octet-stream";
    }
}