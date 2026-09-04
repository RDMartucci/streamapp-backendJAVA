package com.streamapp.streamappbackend.dto;

public record EpisodeDto(
        int episodeNumber,
        String name,
        String overview,
        String airDate,
        String stillUrl,
        Double voteAverage
) {}
