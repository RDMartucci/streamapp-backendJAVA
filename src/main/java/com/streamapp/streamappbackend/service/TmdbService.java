package com.streamapp.streamappbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TmdbService {

    @Value("${app.tmdb.api-key:}")
    private String apiKey;

    @Value("${app.tmdb.image-base-url:https://image.tmdb.org/t/p}")
    private String imageBaseUrl;

    /**
     * Busca un título en TMDB y devuelve la URL del cartel (poster).
     * Retorna null si no hay API key o no encuentra resultados.
     */
    public String searchPoster(String title) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            URL url = new URL("https://api.themoviedb.org/3/search/movie?api_key=" + apiKey +
                    "&query=" + encodedTitle +
                    "&include_adult=false");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            // Parseo simple JSON para extracer poster_path
            String responseBody = response.toString();
            String posterPath = null;
            int posterIdx = responseBody.indexOf("\"poster_path\"");
            if (posterIdx >= 0) {
                int valueStart = responseBody.indexOf('"', posterIdx + 14);
                if (valueStart >= 0) {
                    int valueEnd = responseBody.indexOf('"', valueStart + 1);
                    if (valueEnd > valueStart) {
                        posterPath = responseBody.substring(valueStart + 1, valueEnd);
                    }
                }
            }

            if (posterPath == null || posterPath.isBlank()) {
                return null;
            }

            return imageBaseUrl + "/w500" + posterPath;
        } catch (Exception e) {
            return null;
        }
    }

    public String getImageBaseUrl() {
        return imageBaseUrl;
    }
}