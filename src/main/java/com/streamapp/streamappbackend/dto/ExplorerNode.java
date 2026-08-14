package com.streamapp.streamappbackend.dto;

import com.streamapp.streamappbackend.entity.MediaItem.MediaType;

import java.time.Instant;

public record ExplorerNode(
        String name,
        String path,
        boolean directory,
        MediaType mediaType,
        boolean playable,
        Long sizeBytes,
        Instant lastModified,
        String streamUrl
) {
}