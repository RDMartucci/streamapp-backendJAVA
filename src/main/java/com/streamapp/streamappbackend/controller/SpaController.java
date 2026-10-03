package com.streamapp.streamappbackend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Permite abrir o recargar directamente una ruta del cliente React.
 * Los archivos con extensión (JS, CSS, imágenes) los entrega Spring desde
 * classpath:/static; las rutas de navegación reciben index.html.
 */
@Controller
public class SpaController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping("/{path:^(?!api$|swagger-ui$|v3$)[^.]+$}")
    public String clientRoute() {
        return "forward:/index.html";
    }
}
