package com.streamapp.streamappbackend.dto;

public record SeasonDto(
        int seasonNumber,
        String name,
        int episodeCount,
        String airDate,
        String posterUrl,
        String overview
) {}
