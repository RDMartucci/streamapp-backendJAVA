package com.streamapp.streamappbackend.dto;

/**
 * Resultado de una operación de borrado de contenido por parte de un admin.
 */
public record AdminDeleteResult(
        String path,
        long filesDeleted,
        int catalogItemsRemoved
) {
}