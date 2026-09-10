# StreamApp Backend

Backend de la aplicación de streaming local. Java 17+, Spring Boot 3.2, MySQL y JWT.

## Requisitos

- JDK 17+
- Maven (o usar el wrapper `mvnw`)
- MySQL (el proyecto incluye perfil para Docker) o Docker Desktop
- Contenido de media local al que apuntan las raíces (`app.media.root`)

## Puesta en marcha

### 1. Levantar MySQL con Docker Desktop (Windows 11)

Desde PowerShell, en esta carpeta, ejecutar:

```powershell
.\start-mysql.ps1
```

El script comprueba que Docker Desktop este iniciado, levanta el contenedor
`streamapp-mysql` y espera a que MySQL pase su healthcheck.

Si PowerShell bloquea scripts por la politica de ejecucion, usar una vez:

```powershell
Set-ExecutionPolicy -Scope CurrentUser RemoteSigned
```

Como alternativa manual:

```bash
docker compose up -d --wait mysql
```

### 2. Configurar el contenido

Por defecto el backend sirve `./media` (carpeta junto al `pom.xml`). Se sobreescribe con la variable `MEDIA_ROOT` o en `application.yml`.

### 3. Correr

```bash
./mvnw spring-boot:run
```

Servidor en `http://localhost:8081`. El `DataInitializer` crea el admin de arranque
(por defecto `admin` / `admin1234`, sobreescribible con `ADMIN_USERNAME` / `ADMIN_PASSWORD`).

## Endpoints principales

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/api/auth/register` | Público | Registro de usuario |
| POST | `/api/auth/login` | Público | Login, devuelve JWT |
| GET | `/api/auth/me` | Autenticado | Datos del usuario actual |
| GET | `/api/roots` | Autenticado | Raíces de contenido del usuario |
| POST | `/api/roots` | Autenticado | Agregar raíz (máx. 5) |
| DELETE | `/api/roots?path=...` | Autenticado | Quitar raíz |
| GET | `/api/explorer` | Autenticado | Explorar contenido (navegación, streamUrl firmado) |
| GET | `/api/stream/{ticket}` | Público | Streaming por rangos (HTTP Range, 206) |
| GET/POST | `/api/favorites` | USER/ADMIN | Favoritos del usuario |
| DELETE | `/api/favorites?path=...` | USER/ADMIN | Quitar favorito |
| GET/POST | `/api/playlists` | USER/ADMIN | Playlists del usuario |
| GET/PUT/DELETE | `/api/playlists/{id}` | USER/ADMIN | Ver/renombrar/eliminar playlist |
| POST/DELETE | `/api/playlists/{id}/items` | USER/ADMIN | Agregar/quitar item de playlist |
| GET/POST | `/api/admin/users` | ADMIN | Listar/crear usuarios |
| DELETE | `/api/admin/media?path=...` | ADMIN | Borrar archivo/carpeta (recursivo) dentro de raíces |

## Roles

- `ROLE_VISITOR`: puede loguearse; sin acceso al contenido.
- `ROLE_USER`: exploración, streaming, favoritos y playlists.
- `ROLE_ADMIN`: todo lo anterior + gestión de usuarios y borrado de contenido.

## Seguridad

- JWT en header `Authorization: Bearer <token>` para endpoints protegidos.
- Las rutas `/api/stream/**` son públicas: el ticket en la URL es la credencial
  (firmado con expiración, 30 min por defecto) y habilita el streaming por rangos.
- Rutas del filesystem validadas contra las raíces del usuario (anti-traversal).
- CORS habilitado (ver `SecurityConfig`). Para Bearer token no se usan cookies.

## Swagger / OpenAPI

- UI: `http://localhost:8080/swagger-ui.html`
- Docs JSON: `http://localhost:8080/v3/api-docs`
- Usar el botón **Authorize** con un token de login para probar los endpoints protegidos.

## Configuración por variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/streamapp...` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `streamapp` / `streamapp` | Credenciales DB |
| `MEDIA_ROOT` | `./media` | Directorio base del contenido |
| `JWT_SECRET` | (clave de desarrollo) | Secreto de firma JWT |
| `JWT_EXPIRATION_MS` | `3600000` | Expiración del token (ms) |
| `STREAM_TICKET_TTL_MS` | `1800000` | Vigencia del ticket de streaming (ms) |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `admin1234` | Admin seed |
| `TMDB_API_KEY` | vacío | Clave opcional para metadata de TMDB |

## Formato de errores

Todas las respuestas de error usan el mismo shape:

```json
{
  "error": "NO_AUTH",
  "message": "Autenticación requerida para acceder a este recurso",
  "timestamp": "2026-01-01T00:00:00.000Z"
}
```

Códigos: `NO_AUTH`, `BAD_CREDENTIALS`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`,
`VALIDATION_ERROR`, `BAD_REQUEST`, `INTERNAL_ERROR`.

## Notas

- Rutas en el body/query: usar separador `/` (`D:/media/musica`) o escapar las
  barras en JSON (`D:\\media\\musica`). El backend normaliza.
- El borrado admin (`DELETE /api/admin/media`) es permanente y no usa papelera;
  limpia además el catálogo, favoritos y playlists que referenciaban el contenido.
