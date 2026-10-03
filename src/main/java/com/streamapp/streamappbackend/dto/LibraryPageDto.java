package com.streamapp.streamappbackend.dto;

import java.util.List;

public record LibraryPageDto(
        List<MediaItemDto> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        long movieCount,
        long seriesCount,
        long audioCount
) {
}
