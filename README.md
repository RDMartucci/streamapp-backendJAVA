# StreamApp Backend

Servidor de la aplicación de streaming local. Java 21, Spring Boot, MySQL y JWT.

## Instalación integrada (recomendada)

Esta modalidad crea una única instalación: el servidor entrega tanto la API
como la interfaz web. No hay que ejecutar Node, Vite ni un servidor web
adicional en la computadora que guarda los archivos.

1. Instala Docker Desktop (Windows/macOS) o Docker Engine con Compose (Linux).
2. En esta carpeta, copia `.env.example` a `.env` y reemplaza todos los valores
   de contraseña y secreto.
3. Crea la carpeta `media` aquí y coloca dentro el contenido a compartir. Como
   alternativa, define `MEDIA_PATH` en `.env` con una ruta absoluta de tu disco.
4. Ejecuta `docker compose up --build -d`.
5. Abre `http://localhost:8081` en la computadora servidor. Desde otro equipo
   de la misma red, abre `http://IP_DEL_SERVIDOR:8081`.

La primera vez, inicia sesión con el usuario definido por `ADMIN_USERNAME` y
`ADMIN_PASSWORD`. Al configurar una raíz de contenido dentro de la aplicación,
usa `/media` o una de sus subcarpetas: es la ruta visible dentro del contenedor.

Para permitir acceso desde otros dispositivos, autoriza el puerto elegido
(`STREAMAPP_PORT`, normalmente 8081) en el firewall del equipo servidor. Esta
configuración está pensada para red local; no expongas el puerto directamente a
Internet.

## Requisitos

- JDK 21+
- Maven (o usar el wrapper `mvnw`)
- MySQL (el proyecto incluye perfil para Docker) o Docker Desktop
- Contenido de media local al que apuntan las raíces (`app.media.root`)

## Desarrollo local (avanzado)

### 1. Levantar MySQL con Docker Desktop (Windows 11)

Después de crear el archivo `.env` indicado arriba, levantar solo MySQL con:

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

- UI: `http://localhost:8081/swagger-ui.html`
- Docs JSON: `http://localhost:8081/v3/api-docs`
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

Al iniciar el backend local, `TMDB_API_KEY` se carga automáticamente desde el
archivo `.env` de esta carpeta. También puedes definirla en el entorno; esa
variable tiene prioridad sobre el archivo:

```powershell
.\mvnw.cmd spring-boot:run
```

TMDB proporciona posters y backdrops. StreamApp utiliza el backdrop como fondo
principal de la página de detalle cuando la API devuelve uno.

TMDB es opcional. Si no se configura `TMDB_API_KEY`, la ficha se genera con el
nombre del archivo y, cuando existen junto al vídeo, con estos archivos locales:

```text
pelicula.nfo
poster.jpg | poster.jpeg | poster.png
folder.jpg | folder.jpeg | folder.png
fanart.jpg | fanart.jpeg | fanart.png
backdrop.jpg | backdrop.jpeg | backdrop.png
```

El `.nfo` puede incluir `title`, `originaltitle`, `year`, `plot` y uno o varios
elementos `genre`. La reproducción no depende de metadata externa.

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
