package com.streamapp.streamappbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamapp.streamappbackend.dto.TitleDetailDto;
import com.streamapp.streamappbackend.service.explorer.TitleParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class TitleDetailsService {

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
        if (apiKey == null || apiKey.isBlank()) return null;
        String query = titleParser.clean(rawQuery);
        if (query.isBlank()) query = rawQuery.replaceAll("\\.[a-z0-9]+$", "").replace('.', ' ').trim();
        try {
            String encoded = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            URI uri = URI.create("https://api.themoviedb.org/3/search/multi?api_key=" + apiKey + "&query=" + encoded + "&include_adult=false&language=es-AR");
            HttpRequest req = HttpRequest.newBuilder().uri(uri).GET().header("Accept", "application/json").build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode results = root.get("results");
            if (results == null || !results.isArray() || results.isEmpty()) return null;
            JsonNode first = results.get(0);
            String mediaType = first.path("media_type").asText("movie");
            long tmdbId = first.path("id").asLong();
            if ("tv".equals(mediaType)) return getSeriesDetails((int) tmdbId);
            return getMovieDetails((int) tmdbId);
        } catch (Exception e) {
            return null;
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
            String cast = extractCast(n.get("credits"));
            String directors = extractDirectors(n.get("credits"));
            String trailerUrl = extractTrailer(n.get("videos"));
            return new TitleDetailDto(title, original, year, genres, overview, cast, directors, trailerUrl, posterUrl, "movie", (long) tmdbId);
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
            String cast = extractCast(n.get("aggregate_credits"));
            String directors = extractDirectors(n.get("aggregate_credits"));
            if (directors.isBlank()) directors = joinNames(n.get("created_by"));
            String trailerUrl = extractTrailer(n.get("videos"));
            return new TitleDetailDto(title, original, year, genres, overview, cast, directors, trailerUrl, posterUrl, "series", (long) tmdbId);
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
        for (JsonNode v : arr) {
            if ("Trailer".equals(v.path("type").asText("")) && "YouTube".equals(v.path("site").asText(""))) {
                String key = v.path("key").asText("");
                if (!key.isBlank()) return "https://www.youtube.com/watch?v=" + key;
            }
        }
        return "No disponible";
    }
}
