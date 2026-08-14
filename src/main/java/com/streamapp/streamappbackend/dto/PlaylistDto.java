package com.streamapp.streamappbackend.dto;

import java.time.Instant;
import java.util.List;

/**
 * Vista completa de una playlist con sus items (orden de reproducción).
 */
public record PlaylistDto(
        Long id,
        String name,
        Instant createdAt,
        List<MediaItemDto> items
) {
}