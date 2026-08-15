package com.streamapp.streamappbackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI/Swagger. Expone la UI en /swagger-ui.html y
 * registra el esquema JWT para poder usar el botón "Authorize" sobre los
 * endpoints protegidos.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI streamAppOpenAPI() {
        final String schemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("StreamApp Backend API")
                        .description("""
                                Backend de la app de streaming local.
                                - Auth pública: /api/auth/register y /api/auth/login
                                - Streaming por rangos con ticket firmado: /api/stream/{ticket} (público, sin token)
                                - El resto de los endpoints exige JWT. Algunos solo para ADMIN.
                                Usar el botón "Authorize" con el token devuelto por login.
                                """)
                        .version("0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
