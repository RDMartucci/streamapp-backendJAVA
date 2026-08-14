package com.streamapp.streamappbackend.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Agrega un archivo de media a una playlist identificado por su ruta absoluta.
 */
public class AddPlaylistItemRequest {

    @NotBlank(message = "La ruta del archivo es obligatoria")
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}