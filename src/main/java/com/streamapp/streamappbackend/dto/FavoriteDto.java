package com.streamapp.streamappbackend.dto;

import java.time.Instant;

/**
 * Favorito de un usuario. Reutiliza la representación de MediaItem para que el
 * cliente pueda reproducir directo con streamUrl.
 */
public record FavoriteDto(
        Long id,
        MediaItemDto item,
        Instant createdAt
) {
}