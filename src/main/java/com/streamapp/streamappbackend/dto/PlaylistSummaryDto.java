package com.streamapp.streamappbackend.dto;

import java.time.Instant;
import java.util.List;

/**
 * Vista resumida de una playlist (sin items) para el listado.
 */
public record PlaylistSummaryDto(
        Long id,
        String name,
        Instant createdAt,
        int itemCount
) {
}