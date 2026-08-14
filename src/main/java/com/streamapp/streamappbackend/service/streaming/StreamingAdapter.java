package com.streamapp.streamappbackend.service.streaming;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.file.Path;

/**
 * Estrategia de entrega de contenido de streaming.
 * La fase 1 implementa RangeStreaming (HTTP Range / seek / Accept-Ranges).
 * A futuro se puede agregar una implementación HLS adaptativa sin tocar
 * los controladores ni entidades, solo añadiendo otro adapter.
 */
public interface StreamingAdapter {

    ResponseEntity<StreamingResponseBody> stream(Path resolvedPath, String rangeHeader);
}