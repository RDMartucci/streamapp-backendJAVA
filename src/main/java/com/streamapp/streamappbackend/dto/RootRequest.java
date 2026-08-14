package com.streamapp.streamappbackend.dto;

import jakarta.validation.constraints.NotBlank;

public class RootRequest {

    @NotBlank(message = "La ruta es obligatoria")
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}