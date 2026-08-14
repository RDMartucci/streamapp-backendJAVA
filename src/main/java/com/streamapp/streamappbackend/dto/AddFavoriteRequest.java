package com.streamapp.streamappbackend.dto;

import jakarta.validation.constraints.NotBlank;

public class AddFavoriteRequest {

    @NotBlank(message = "La ruta del archivo es obligatoria")
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}