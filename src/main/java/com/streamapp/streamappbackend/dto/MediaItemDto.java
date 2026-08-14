package com.streamapp.streamappbackend.dto;

import com.streamapp.streamappbackend.entity.MediaItem;

import java.time.Instant;

/**
 * Elemento de media ya registrado en el catálogo (o a registrar), tal como se
 * expone en favoritos y playlists. Incluye un streamUrl fresco para reproducir.
 */
public record MediaItemDto(
        Long id,
        String title,
        String path,
        MediaItem.MediaType mediaType,
        String contentType,
        long size,
        Instant lastModified,
        String streamUrl
) {
}