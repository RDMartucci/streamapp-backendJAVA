package com.streamapp.streamappbackend.dto;

public record LibraryScanResult(
        int indexed,
        int updated,
        int skipped
) {
}
