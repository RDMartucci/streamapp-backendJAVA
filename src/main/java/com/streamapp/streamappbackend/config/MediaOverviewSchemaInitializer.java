package com.streamapp.streamappbackend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MediaOverviewSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public MediaOverviewSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String columnType = jdbcTemplate.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                String.class, "media_items", "overview");
        if ("varchar".equalsIgnoreCase(columnType)) {
            jdbcTemplate.execute("ALTER TABLE media_items MODIFY COLUMN overview TEXT");
        }
    }
}