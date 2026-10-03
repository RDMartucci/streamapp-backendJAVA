package com.streamapp.streamappbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamapp.streamappbackend.dto.TitleDetailDto;
import com.streamapp.streamappbackend.service.explorer.TitleParser;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TitleDetailsService {

    private static final Logger log = LoggerFactory.getLogger(TitleDetailsService.class);

    @Value("${app.tmdb.api-key:}")
    private String apiKey;

    @Value("${app.tmdb.image-base-url:https://image.tmdb.org/t/p}")
    private String imageBaseUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TitleParser titleParser;

    public TitleDetailsService(ObjectMapper objectMapper, TitleParser titleParser) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.titleParser = titleParser;
    }

    public TitleDetailDto searchAndFormat(String rawQuery) {
        Integer fileYear = extractYear(rawQuery);
        for (String query : queryVariants(rawQuery)) {
            JsonNode results = searchResults(query);
            if (results == null || results.isEmpty()) {
                continue;
            }
            JsonNode chosen = pickByYear(results, fileYear);
            String mediaType = chosen.path("media_type").asText("movie");
            long tmdbId = chosen.path("id").asLong();
            TitleDetailDto detail = "tv".equals(mediaType)
                    ? getSeriesDetails((int) tmdbId)
                    : getMovieDetails((int) tmdbId);
            if (detail != null) {
                return detail;
            }
        }
        return null;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public TitleDetailDto basicDetails(String rawQuery, String rawPath) {
        String title = titleParser.clean(rawQuery);
        if (title == null || title.isBlank()) {
            title = rawQuery == null ? "Título sin nombre" : rawQuery;
        }
        String original = title;
        Integer year = extractYear(rawQuery == null ? "" : rawQuery);
        String overview = "";
        String genres = "";
        String posterUrl = null;
        String backdropUrl = null;
        if (rawPath != null && !rawPath.isBlank()) {
            Path media = Path.of(rawPath);
            Path nfo = sibling(media, ".nfo");
            if (Files.isRegularFile(nfo)) {
                try {
                    String xml = Files.readString(nfo);
                    title = xmlValue(xml, "title", title);
                    original = xmlValue(xml, "originaltitle", original);
                    overview = xmlValue(xml, "plot", "");
                    genres = xmlValues(xml, "genre");
                    String nfoYear = xmlValue(xml, "year", "");
                    if (!nfoYear.isBlank()) year = Integer.parseInt(nfoYear);
                } catch (java.io.IOException | NumberFormatException e) {
                    log.warn("No se pudo leer metadata local {}", nfo, e);
                }
            }
            if (Files.isRegularFile(sibling(media, "poster.jpg"))) {
                posterUrl = localImageUrl(rawPath, "poster");
            } else if (Files.isRegularFile(sibling(media, "folder.jpg"))) {
                posterUrl = localImageUrl(rawPath, "folder");
            }
            if (Files.isRegularFile(sibling(media, "fanart.jpg"))) {
                backdropUrl = localImageUrl(rawPath, "fanart");
            } else if (Files.isRegularFile(sibling(media, "backdrop.jpg"))) {
                backdropUrl = localImageUrl(rawPath, "backdrop");
            }
        }
        String mediaType = rawQuery != null && titleParser.mediaTypeDetail(rawQuery, true).equals("series")
                ? "series" : "movie";
        return new TitleDetailDto(title, original, year, genres, overview, "", "", null,
                posterUrl, mediaType, null, null, null, null, null, null, null, backdropUrl);
    }

    private Path sibling(Path media, String filename) {
        return media.toAbsolutePath().normalize().getParent().resolve(filename);
    }

    private String localImageUrl(String mediaPath, String kind) {
        return "/api/tmdb/local-image?path=" + java.net.URLEncoder.encode(mediaPath, java.nio.charset.StandardCharsets.UTF_8)
                + "&kind=" + kind;
    }

    private String xmlValue(String xml, String tag, String fallback) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("<" + tag + ">\\s*(.*?)\\s*</" + tag + ">", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL)
                .matcher(xml);
        return matcher.find() ? matcher.group(1).trim() : fallback;
    }

    private String xmlValues(String xml, String tag) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("<" + tag + ">\\s*(.*?)\\s*</" + tag + ">", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL)
                .matcher(xml);
        java.util.List<String> values = new java.util.ArrayList<>();
        while (matcher.find()) values.add(matcher.group(1).trim());
        return String.join(", ", values);
    }

    private java.util.List<String> queryVariants(String rawQuery) {
        java.util.LinkedHashSet<String> variants = new java.util.LinkedHashSet<>();
        if (rawQuery != null && !rawQuery.isBlank()) {
            String withoutExtension = rawQuery.replaceAll("\\.[a-z0-9]+$", "").trim();
            variants.add(titleParser.clean(rawQuery));
            variants.add(withoutExtension.replace('.', ' ').trim());
            variants.add(withoutExtension);
        }
        variants.removeIf(String::isBlank);
        return new java.util.ArrayList<>(variants);
    }

    private JsonNode searchResults(String query) {
        if (!isConfigured()) {
            return null;
        }
        try {
            String encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            URI uri = URI.create("https://api.themoviedb.org/3/search/multi?api_key=" + apiKey
                    + "&query=" + encoded + "&include_adult=false&language=es-AR");
            HttpRequest req = HttpRequest.newBuilder().uri(uri).GET()
                    .header("Accept", "application/json").build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("TMDB search failed with HTTP {} for query '{}'", resp.statusCode(), query);
                return null;
            }
            JsonNode results = objectMapper.readTree(resp.body()).get("results");
            return results != null && results.isArray() ? results : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("TMDB search interrupted for query '{}'", query);
            return null;
        } catch (java.io.IOException | RuntimeException e) {
            log.warn("TMDB search failed for query '{}': {}", query, e.getMessage());
            return null;
        }
    }

    private Integer extractYear(String raw) {
        var m = java.util.regex.Pattern.compile("\\b(19|20)\\d{2}\\b").matcher(raw);
        if (m.find()) try { return Integer.parseInt(m.group()); } catch (NumberFormatException ignored) {}
        return null;
    }

    private JsonNode pickByYear(JsonNode results, Integer fileYear) {
        if (fileYear == null || !results.isArray() || results.isEmpty()) return results.get(0);
        for (JsonNode n : results) {
            String date = n.has("release_date") ? n.path("release_date").asText("") : n.path("first_air_date").asText("");
            if (date.length() >= 4) {
                try {
                    int y = Integer.parseInt(date.substring(0, 4));
                    if (y == fileYear) return n;
                } catch (NumberFormatException ignored) {}
            }
        }
        return results.get(0);
    }

    public java.util.List<TitleDetailDto> searchCandidates(String rawQuery) {
        if (!isConfigured()) {
            log.warn("No se pueden buscar coincidencias TMDB: TMDB_API_KEY no está configurada");
            return java.util.Collections.emptyList();
        }
        java.util.LinkedHashMap<String, TitleDetailDto> candidates = new java.util.LinkedHashMap<>();
        try {
            for (String query : queryVariants(rawQuery)) {
                JsonNode results = searchResults(query);
                if (results == null || !results.isArray()) continue;
                int limit = Math.min(results.size(), 10);
                for (int i = 0; i < limit && candidates.size() < 12; i++) {
                    JsonNode n = results.get(i);
                String mediaType = n.path("media_type").asText("movie");
                if (!"movie".equals(mediaType) && !"tv".equals(mediaType)) continue;
                long tmdbId = n.path("id").asLong();
                String title = "tv".equals(mediaType) ? n.path("name").asText("") : n.path("title").asText("");
                String original = "tv".equals(mediaType) ? n.path("original_name").asText(title) : n.path("original_title").asText(title);
                String date = "tv".equals(mediaType) ? n.path("first_air_date").asText("") : n.path("release_date").asText("");
                Integer year = date.length() >= 4 ? Integer.parseInt(date.substring(0, 4)) : null;
                String poster = n.path("poster_path").asText(null);
                String posterUrl = poster != null && !poster.equals("null") && !poster.isBlank() ? imageBaseUrl + "/w185" + poster : null;
                Double vote = n.path("vote_average").isNumber() ? n.path("vote_average").asDouble() : null;
                String overview = n.path("overview").asText("");
                    candidates.putIfAbsent(mediaType + ":" + tmdbId,
                            new TitleDetailDto(title, original, year, "", overview, "", "", null, posterUrl,
                                    mediaType.equals("tv") ? "series" : "movie", tmdbId, vote, null, null, null, null, date, null));
                }
            }
            return new java.util.ArrayList<>(candidates.values());
        } catch (RuntimeException e) {
            log.warn("Error al buscar coincidencias TMDB para '{}': {}", rawQuery, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    public TitleDetailDto getMovieDetails(int tmdbId) {
        if (apiKey == null || apiKey.isBlank()) return null;
        try {
            URI uri = URI.create("https://api.themoviedb.org/3/movie/" + tmdbId + "?api_key=" + apiKey + "&language=es-AR&append_to_response=credits,videos");
            HttpRequest req = HttpRequest.newBuilder().uri(uri).GET().header("Accept", "application/json").build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            JsonNode n = objectMapper.readTree(resp.body());
            String title = n.path("title").asText("");
            String original = n.path("original_title").asText(title);
            String release = n.path("release_date").asText("");
            Integer year = release.length() >= 4 ? Integer.parseInt(release.substring(0,4)) : null;
            String genres = joinNames(n.get("genres"));
            String overview = n.path("overview").asText("");
            String poster = n.path("poster_path").asText(null);
            String posterUrl = poster != null && !poster.equals("null") ? imageBaseUrl + "/w342" + poster : null;
            String backdrop = n.path("backdrop_path").asText(null);
            String backdropUrl = backdrop != null && !backdrop.equals("null") && !backdrop.isBlank() ? imageBaseUrl + "/w1280" + backdrop : null;
            Double vote = n.path("vote_average").isNumber() ? n.path("vote_average").asDouble() : null;
            Integer runtime = n.path("runtime").isInt() ? n.path("runtime").asInt() : null;
            String countries = joinNames(n.get("production_countries"));
            if (countries.isBlank()) countries = joinNames(n.get("origin_country"));
            String studios = joinNames(n.get("production_companies"));
            String releaseDate = n.path("release_date").asText(null);
            String cast = extractCast(n.get("credits"));
            String directors = extractDirectors(n.get("credits"));
            String trailerUrl = extractTrailer(n.get("videos"));
            return new TitleDetailDto(title, original, year, genres, overview, cast, directors, trailerUrl, posterUrl, "movie", (long) tmdbId, vote, null, runtime, countries, studios, releaseDate, backdropUrl);
        } catch (Exception e) {
            return null;
        }
    }

    public TitleDetailDto getSeriesDetails(int tmdbId) {
        if (apiKey == null || apiKey.isBlank()) return null;
        try {
            URI uri = URI.create("https://api.themoviedb.org/3/tv/" + tmdbId + "?api_key=" + apiKey + "&language=es-AR&append_to_response=aggregate_credits,videos");
            HttpRequest req = HttpRequest.newBuilder().uri(uri).GET().header("Accept", "application/json").build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            JsonNode n = objectMapper.readTree(resp.body());
            String title = n.path("name").asText("");
            String original = n.path("original_name").asText(title);
            String firstAir = n.path("first_air_date").asText("");
            Integer year = firstAir.length() >= 4 ? Integer.parseInt(firstAir.substring(0,4)) : null;
            String genres = joinNames(n.get("genres"));
            String overview = n.path("overview").asText("");
            String poster = n.path("poster_path").asText(null);
            String posterUrl = poster != null && !poster.equals("null") ? imageBaseUrl + "/w342" + poster : null;
            String backdrop = n.path("backdrop_path").asText(null);
            String backdropUrl = backdrop != null && !backdrop.equals("null") && !backdrop.isBlank() ? imageBaseUrl + "/w1280" + backdrop : null;
            Double vote = n.path("vote_average").isNumber() ? n.path("vote_average").asDouble() : null;
            String cast = extractCast(n.get("aggregate_credits"));
            String directors = extractDirectors(n.get("aggregate_credits"));
            if (directors.isBlank()) directors = joinNames(n.get("created_by"));
            String trailerUrl = extractTrailer(n.get("videos"));
            java.util.List<com.streamapp.streamappbackend.dto.SeasonDto> seasons = parseSeasons(n.get("seasons"));
            Integer runtime = null;
            JsonNode runtimes = n.get("episode_run_time");
            if (runtimes != null && runtimes.isArray() && !runtimes.isEmpty()) runtime = runtimes.get(0).asInt();
            String countries = joinNames(n.get("production_countries"));
            if (countries.isBlank()) countries = joinNames(n.get("origin_country"));
            String studios = joinNames(n.get("production_companies"));
            if (studios.isBlank()) studios = joinNames(n.get("networks"));
            String releaseDate = n.path("first_air_date").asText(null);
            return new TitleDetailDto(title, original, year, genres, overview, cast, directors, trailerUrl, posterUrl, "series", (long) tmdbId, vote, seasons, runtime, countries, studios, releaseDate, backdropUrl);
        } catch (Exception e) {
            return null;
        }
    }

    private String joinNames(JsonNode arr) {
        if (arr == null || !arr.isArray() || arr.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode el : arr) {
            String name = el.path("name").asText("");
            if (!name.isBlank()) sb.append(name).append(", ");
        }
        if (sb.length() >= 2) sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private String extractCast(JsonNode credits) {
        if (credits == null) return "";
        JsonNode cast = credits.get("cast");
        if (cast == null || !cast.isArray()) return "";
        StringBuilder sb = new StringBuilder();
        int c = 0;
        for (JsonNode p : cast) {
            if (c++ >= 5) break;
            String name = p.path("name").asText("");
            if (!name.isBlank()) sb.append(name).append(", ");
        }
        if (sb.length() >= 2) sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private String extractDirectors(JsonNode credits) {
        if (credits == null) return "";
        JsonNode crew = credits.get("crew");
        if (crew == null || !crew.isArray()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode p : crew) {
            if ("Directing".equals(p.path("department").asText("")) || "Director".equals(p.path("job").asText(""))) {
                String name = p.path("name").asText("");
                if (!name.isBlank() && sb.indexOf(name) == -1) sb.append(name).append(", ");
            }
        }
        if (sb.length() >= 2) sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private String extractTrailer(JsonNode videos) {
        if (videos == null) return "No disponible";
        JsonNode results = videos.get("results");
        JsonNode arr = results != null ? results : videos;
        if (arr == null || !arr.isArray()) return "No disponible";
        String fallback = null;
        for (JsonNode v : arr) {
            if (!"Trailer".equals(v.path("type").asText("")) || !"YouTube".equals(v.path("site").asText(""))) continue;
            String key = v.path("key").asText("");
            if (key.isBlank()) continue;
            String lang = v.path("iso_639_1").asText("");
            if ("es".equals(lang)) return "https://www.youtube.com/watch?v=" + key;
            if (fallback == null) fallback = "https://www.youtube.com/watch?v=" + key;
        }
        return fallback != null ? fallback : "No disponible";
    }

    private java.util.List<com.streamapp.streamappbackend.dto.SeasonDto> parseSeasons(JsonNode arr) {
        if (arr == null || !arr.isArray() || arr.isEmpty()) return null;
        java.util.List<com.streamapp.streamappbackend.dto.SeasonDto> list = new java.util.ArrayList<>();
        for (JsonNode s : arr) {
            int num = s.path("season_number").asInt(0);
            String name = s.path("name").asText("Temporada " + num);
            int eps = s.path("episode_count").asInt(0);
            String air = s.path("air_date").asText(null);
            String poster = s.path("poster_path").asText(null);
            String posterUrl = poster != null && !poster.equals("null") && !poster.isBlank() ? imageBaseUrl + "/w185" + poster : null;
            String overview = s.path("overview").asText("");
            list.add(new com.streamapp.streamappbackend.dto.SeasonDto(num, name, eps, air, posterUrl, overview));
        }
        return list;
    }

    public java.util.List<com.streamapp.streamappbackend.dto.EpisodeDto> getSeasonEpisodes(int tmdbId, int seasonNumber) {
        if (apiKey == null || apiKey.isBlank()) return java.util.Collections.emptyList();
        try {
            URI uri = URI.create("https://api.themoviedb.org/3/tv/" + tmdbId + "/season/" + seasonNumber + "?api_key=" + apiKey + "&language=es-AR");
            HttpRequest req = HttpRequest.newBuilder().uri(uri).GET().header("Accept", "application/json").build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return java.util.Collections.emptyList();
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode eps = root.get("episodes");
            if (eps == null || !eps.isArray()) return java.util.Collections.emptyList();
            java.util.List<com.streamapp.streamappbackend.dto.EpisodeDto> out = new java.util.ArrayList<>();
            for (JsonNode e : eps) {
                int epNum = e.path("episode_number").asInt(0);
                String name = e.path("name").asText("Episodio " + epNum);
                String overview = e.path("overview").asText("");
                String air = e.path("air_date").asText(null);
                String still = e.path("still_path").asText(null);
                String stillUrl = still != null && !still.equals("null") && !still.isBlank() ? imageBaseUrl + "/w300" + still : null;
                Double vote = e.path("vote_average").isNumber() ? e.path("vote_average").asDouble() : null;
                out.add(new com.streamapp.streamappbackend.dto.EpisodeDto(epNum, name, overview, air, stillUrl, vote));
            }
            return out;
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}
