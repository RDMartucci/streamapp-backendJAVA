package com.streamapp.streamappbackend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TmdbApiKeyEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TmdbApiKeyEnvironmentPostProcessor.class);
    private static final String PROPERTY_NAME = "TMDB_API_KEY";
    private static final String PROPERTY_SOURCE_NAME = "backendDotEnvTmdb";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String configuredKey = environment.getProperty(PROPERTY_NAME);
        if (configuredKey != null && !configuredKey.isBlank()) {
            return;
        }

        Path envFile = findEnvFile();
        if (envFile == null) {
            return;
        }

        Properties properties = new Properties();
        try (var input = Files.newInputStream(envFile)) {
            properties.load(input);
            String apiKey = unquote(properties.getProperty(PROPERTY_NAME, "").trim());
            if (!apiKey.isBlank()) {
                PropertySource<?> source = new MapPropertySource(
                        PROPERTY_SOURCE_NAME, Map.of(PROPERTY_NAME, apiKey));
                environment.getPropertySources().addLast(source);
                log.info("Loaded TMDB_API_KEY from {}", envFile.toAbsolutePath());
            }
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Could not read TMDB_API_KEY from {}", envFile.toAbsolutePath());
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private Path findEnvFile() {
        for (Path candidate : List.of(Path.of(".env"), Path.of("streamapp-backend", ".env"))) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }
}